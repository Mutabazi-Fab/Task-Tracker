package com.throughline.taskmanagement.service.impl;

import com.throughline.taskmanagement.dto.request.LoginRequest;
import com.throughline.taskmanagement.dto.request.ForgotPasswordRequest;
import com.throughline.taskmanagement.dto.request.ResendOtpRequest;
import com.throughline.taskmanagement.dto.request.ResetPasswordRequest;
import com.throughline.taskmanagement.dto.request.SignUpRequest;
import com.throughline.taskmanagement.dto.request.VerifyEmailRequest;
import com.throughline.taskmanagement.dto.response.AuthResponse;
import com.throughline.taskmanagement.dto.response.PersonResponse;
import com.throughline.taskmanagement.exception.EmailDeliveryException;
import com.throughline.taskmanagement.exception.ForbiddenActionException;
import com.throughline.taskmanagement.exception.InvalidAssignmentException;
import com.throughline.taskmanagement.exception.InvalidCredentialsException;
import com.throughline.taskmanagement.exception.ResourceNotFoundException;
import com.throughline.taskmanagement.mapper.PersonMapper;
import com.throughline.taskmanagement.model.Person;
import com.throughline.taskmanagement.repository.PersonRepository;
import com.throughline.taskmanagement.repository.TeamMemberRepository;
import com.throughline.taskmanagement.security.JwtService;
import com.throughline.taskmanagement.security.LoginRateLimiter;
import com.throughline.taskmanagement.service.AuthService;
import com.throughline.taskmanagement.service.MailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final int OTP_TTL_MINUTES = 10;
    private static final int OTP_RESEND_COOLDOWN_SECONDS = 60;
    private static final int RESET_CODE_TTL_MINUTES = 10;
    private static final int RESET_CODE_RESEND_COOLDOWN_SECONDS = 60;
    private static final SecureRandom OTP_RANDOM = new SecureRandom();

    private final PersonRepository personRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final PersonMapper personMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final MailService mailService;
    private final LoginRateLimiter rateLimiter;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    public AuthResponse login(LoginRequest request) {
        // Checked before anything else — a locked-out email can't be used to keep
        // grinding through passwords no matter what else is true about the account.
        rateLimiter.checkAllowed(request.email());

        Person person = personRepository.findByEmailIgnoreCase(request.email()).orElse(null);
        if (person == null) {
            rateLimiter.recordFailure(request.email());
            throw new InvalidCredentialsException("Invalid email or password.");
        }

        if (person.getPassword() == null) {
            // A Super Admin created this account but it hasn't been signed into yet — this
            // isn't a wrong guess, so it doesn't count against the rate limit, same as the
            // deactivated/unverified checks below. Distinct message so the frontend can
            // route to sign-up instead of showing a generic error.
            throw new ForbiddenActionException("Please finish signing up before logging in.");
        }

        if (!passwordEncoder.matches(request.password(), person.getPassword())) {
            rateLimiter.recordFailure(request.email());
            throw new InvalidCredentialsException("Invalid email or password.");
        }

        if (!person.isActive()) {
            throw new ForbiddenActionException("This account has been deactivated.");
        }

        if (!person.isEmailVerified()) {
            // Distinct message from the generic "invalid credentials" above, so the
            // frontend can route to "verify your email" instead of just showing an error.
            // Doesn't auto-resend here — repeated failed login attempts shouldn't spam
            // their inbox; they use the dedicated resend endpoint for that.
            throw new ForbiddenActionException("Please verify your email before logging in.");
        }

        rateLimiter.recordSuccess(request.email());
        String token = jwtService.generateToken(person.getEmail());
        return new AuthResponse(token, person.getId(), person.getFullName(), person.getEmail(), person.getRole(), true);
    }

    @Override
    public AuthResponse verifyEmail(VerifyEmailRequest request) {
        rateLimiter.checkAllowed(request.email());

        Person person = personRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("Person not found"));

        if (person.isEmailVerified()) {
            throw new InvalidAssignmentException("This email is already verified.");
        }
        if (person.getOtpCode() == null || person.getOtpExpiresAt() == null
                || person.getOtpExpiresAt().isBefore(LocalDateTime.now())) {
            rateLimiter.recordFailure(request.email());
            throw new InvalidCredentialsException("This code has expired. Request a new one.");
        }
        if (!person.getOtpCode().equals(request.otp())) {
            rateLimiter.recordFailure(request.email());
            throw new InvalidCredentialsException("Incorrect verification code.");
        }

        person.setEmailVerified(true);
        person.setOtpCode(null);
        person.setOtpExpiresAt(null);
        Person saved = personRepository.save(person);

        rateLimiter.recordSuccess(request.email());
        String token = jwtService.generateToken(saved.getEmail());
        return new AuthResponse(token, saved.getId(), saved.getFullName(), saved.getEmail(), saved.getRole(), true);
    }

    @Override
    public void resendOtp(ResendOtpRequest request) {
        Person person = personRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("Person not found"));

        if (person.isEmailVerified()) {
            throw new InvalidAssignmentException("This email is already verified.");
        }

        // otpExpiresAt = sentAt + OTP_TTL_MINUTES, so "sent less than COOLDOWN ago" is the
        // same as "expiresAt is still more than (TTL - COOLDOWN) away" — no separate
        // "last sent" column needed just for this.
        if (person.getOtpExpiresAt() != null) {
            LocalDateTime cooldownEndsAt = person.getOtpExpiresAt()
                    .minusMinutes(OTP_TTL_MINUTES)
                    .plusSeconds(OTP_RESEND_COOLDOWN_SECONDS);
            if (cooldownEndsAt.isAfter(LocalDateTime.now())) {
                throw new InvalidAssignmentException("Please wait before requesting another code.");
            }
        }

        sendSignUpCode(person);
    }

    @Override
    public AuthResponse signUp(SignUpRequest request) {
        // Same brute-force shape as verifyEmail/resetPassword — a 6-digit code an attacker
        // could grind through given enough attempts — so it gets the same guard.
        rateLimiter.checkAllowed(request.email());

        Person person = personRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("Person not found"));

        if (person.getPassword() != null) {
            throw new InvalidAssignmentException("This account has already signed up. Try logging in instead.");
        }
        if (person.getOtpCode() == null || person.getOtpExpiresAt() == null
                || person.getOtpExpiresAt().isBefore(LocalDateTime.now())) {
            rateLimiter.recordFailure(request.email());
            throw new InvalidCredentialsException("This code has expired. Request a new one.");
        }
        if (!person.getOtpCode().equals(request.otp())) {
            rateLimiter.recordFailure(request.email());
            throw new InvalidCredentialsException("Incorrect sign-up code.");
        }

        rateLimiter.recordSuccess(request.email());
        person.setPassword(passwordEncoder.encode(request.newPassword()));
        person.setEmailVerified(true);
        person.setOtpCode(null);
        person.setOtpExpiresAt(null);
        Person saved = personRepository.save(person);

        String token = jwtService.generateToken(saved.getEmail());
        return new AuthResponse(token, saved.getId(), saved.getFullName(), saved.getEmail(), saved.getRole(), true);
    }

    @Override
    public void forgotPassword(ForgotPasswordRequest request) {
        Person person = personRepository.findByEmailIgnoreCase(request.email()).orElse(null);

        // Deliberately silent for unknown email, a never-claimed account, or the resend
        // cooldown — same generic outcome either way, so this can't probe registered emails.
        if (person == null || person.getPassword() == null) {
            return;
        }
        if (person.getResetCodeExpiresAt() != null) {
            LocalDateTime cooldownEndsAt = person.getResetCodeExpiresAt()
                    .minusMinutes(RESET_CODE_TTL_MINUTES)
                    .plusSeconds(RESET_CODE_RESEND_COOLDOWN_SECONDS);
            if (cooldownEndsAt.isAfter(LocalDateTime.now())) {
                return;
            }
        }

        sendPasswordResetCode(person);
    }

    @Override
    public AuthResponse resetPassword(ResetPasswordRequest request) {
        // Same brute-force shape as verifyEmail — a 6-digit code an attacker could grind
        // through given enough attempts — so it gets the same guard.
        rateLimiter.checkAllowed(request.email());

        Person person = personRepository.findByEmailIgnoreCase(request.email()).orElse(null);
        if (person == null) {
            rateLimiter.recordFailure(request.email());
            throw new InvalidCredentialsException("Invalid email or code.");
        }

        if (person.getResetCode() == null || person.getResetCodeExpiresAt() == null
                || person.getResetCodeExpiresAt().isBefore(LocalDateTime.now())) {
            rateLimiter.recordFailure(request.email());
            throw new InvalidCredentialsException("This code has expired. Request a new one.");
        }
        if (!person.getResetCode().equals(request.code())) {
            rateLimiter.recordFailure(request.email());
            throw new InvalidCredentialsException("Incorrect reset code.");
        }

        rateLimiter.recordSuccess(request.email());
        person.setPassword(passwordEncoder.encode(request.newPassword()));
        person.setResetCode(null);
        person.setResetCodeExpiresAt(null);
        Person saved = personRepository.save(person);

        // Best-effort — the password change itself already succeeded; a flaky confirmation
        // email shouldn't undo that or block them from logging in with it.
        try {
            mailService.send(
                    saved.getEmail(),
                    "Your Throughline password was changed",
                    "Your password was just changed. If this wasn't you, contact your administrator immediately.");
        } catch (Exception e) {
            // Ignored on purpose — see comment above.
        }

        String token = jwtService.generateToken(saved.getEmail());
        return new AuthResponse(token, saved.getId(), saved.getFullName(), saved.getEmail(), saved.getRole(), saved.isEmailVerified());
    }

    @Override
    public void sendPasswordResetCode(Person person) {
        String code = String.format("%06d", OTP_RANDOM.nextInt(1_000_000));
        person.setResetCode(code);
        person.setResetCodeExpiresAt(LocalDateTime.now().plusMinutes(RESET_CODE_TTL_MINUTES));
        personRepository.save(person);

        try {
            mailService.sendCode(
                    person.getEmail(),
                    "Reset your Throughline password",
                    "Enter this code to reset your password. It expires in " + RESET_CODE_TTL_MINUTES + " minutes.",
                    code,
                    frontendUrl + "/reset-password",
                    "Reset password");
        } catch (Exception e) {
            throw new EmailDeliveryException("Could not send the password reset email. Please try again.");
        }
    }

    @Override
    public PersonResponse getCurrentPerson(String email) {
        Person person = personRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Person not found"));
        return personMapper.toResponse(person, teamMemberRepository.findByPersonId(person.getId()));
    }

    @Override
    public void sendSignUpCode(Person person) {
        String otp = String.format("%06d", OTP_RANDOM.nextInt(1_000_000));
        person.setOtpCode(otp);
        person.setOtpExpiresAt(LocalDateTime.now().plusMinutes(OTP_TTL_MINUTES));
        personRepository.save(person);

        try {
            mailService.sendCode(
                    person.getEmail(),
                    "Complete your Throughline sign-up",
                    "You've been added to Throughline. Enter this code to set your password. It expires in "
                            + OTP_TTL_MINUTES + " minutes.",
                    otp,
                    frontendUrl + "/sign-up",
                    "Complete sign-up");
        } catch (Exception e) {
            throw new EmailDeliveryException("Could not send the sign-up email. Please try again.");
        }
    }
}
