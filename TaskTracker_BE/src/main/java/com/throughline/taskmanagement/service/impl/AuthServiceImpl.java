package com.throughline.taskmanagement.service.impl;

import com.throughline.taskmanagement.dto.request.LoginRequest;
import com.throughline.taskmanagement.dto.request.ResendOtpRequest;
import com.throughline.taskmanagement.dto.request.SignupRequest;
import com.throughline.taskmanagement.dto.request.VerifyEmailRequest;
import com.throughline.taskmanagement.dto.response.AuthResponse;
import com.throughline.taskmanagement.dto.response.PersonResponse;
import com.throughline.taskmanagement.enums.Role;
import com.throughline.taskmanagement.exception.DuplicateResourceException;
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
import com.throughline.taskmanagement.service.AuthService;
import com.throughline.taskmanagement.service.MailService;
import lombok.RequiredArgsConstructor;
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
    private static final SecureRandom OTP_RANDOM = new SecureRandom();

    private final PersonRepository personRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final PersonMapper personMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final MailService mailService;

    @Override
    public AuthResponse signup(SignupRequest request) {
        Person person = personRepository.findByEmailIgnoreCase(request.email()).orElse(null);

        if (person != null && person.getPassword() != null) {
            throw new DuplicateResourceException("An account with this email already exists.");
        }

        if (person == null) {
            person = new Person();
            person.setEmail(request.email());
            // Brand new to the system — must prove they control this inbox before they
            // can log in. A pre-existing (seeded) record being claimed instead keeps
            // whatever emailVerified value it already had (true, for anything that
            // predates this feature — see the @ColumnDefault on Person.emailVerified).
            person.setEmailVerified(false);
        }
        // else: this claims a pre-existing record (e.g. seeded before auth existed, or added
        // to a team by a Director before ever signing up) instead of creating a duplicate
        // person with the same email.

        person.setFullName(request.fullName());
        person.setJobTitle(request.jobTitle());
        person.setRank(request.rank());
        person.setPassword(passwordEncoder.encode(request.password()));
        if (person.getRole() == null) {
            person.setRole(Role.MEMBER);
        }

        Person saved = personRepository.save(person);

        if (!saved.isEmailVerified()) {
            sendOtp(saved);
            return new AuthResponse(null, saved.getId(), saved.getFullName(), saved.getEmail(), saved.getRole(), false);
        }

        String token = jwtService.generateToken(saved.getEmail());
        return new AuthResponse(token, saved.getId(), saved.getFullName(), saved.getEmail(), saved.getRole(), true);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        Person person = personRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password."));

        if (person.getPassword() == null || !passwordEncoder.matches(request.password(), person.getPassword())) {
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

        String token = jwtService.generateToken(person.getEmail());
        return new AuthResponse(token, person.getId(), person.getFullName(), person.getEmail(), person.getRole(), true);
    }

    @Override
    public AuthResponse verifyEmail(VerifyEmailRequest request) {
        Person person = personRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("Person not found"));

        if (person.isEmailVerified()) {
            throw new InvalidAssignmentException("This email is already verified.");
        }
        if (person.getOtpCode() == null || person.getOtpExpiresAt() == null
                || person.getOtpExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidCredentialsException("This code has expired. Request a new one.");
        }
        if (!person.getOtpCode().equals(request.otp())) {
            throw new InvalidCredentialsException("Incorrect verification code.");
        }

        person.setEmailVerified(true);
        person.setOtpCode(null);
        person.setOtpExpiresAt(null);
        Person saved = personRepository.save(person);

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

        sendOtp(person);
    }

    @Override
    public PersonResponse getCurrentPerson(String email) {
        Person person = personRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Person not found"));
        return personMapper.toResponse(person, teamMemberRepository.findByPersonId(person.getId()));
    }

    private void sendOtp(Person person) {
        String otp = String.format("%06d", OTP_RANDOM.nextInt(1_000_000));
        person.setOtpCode(otp);
        person.setOtpExpiresAt(LocalDateTime.now().plusMinutes(OTP_TTL_MINUTES));
        personRepository.save(person);

        try {
            mailService.send(
                    person.getEmail(),
                    "Your Throughline verification code",
                    "Your verification code is " + otp + ". It expires in " + OTP_TTL_MINUTES + " minutes."
            );
        } catch (Exception e) {
            throw new EmailDeliveryException("Could not send the verification email. Please try again.");
        }
    }
}
