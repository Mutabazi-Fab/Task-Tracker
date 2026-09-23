package com.throughline.taskmanagement.service.impl;

import com.throughline.taskmanagement.dto.request.LoginRequest;
import com.throughline.taskmanagement.dto.request.PasswordResetEmailRequest;
import com.throughline.taskmanagement.dto.request.TotpConfirmRequest;
import com.throughline.taskmanagement.dto.request.TotpVerifyRequest;
import com.throughline.taskmanagement.dto.response.AuthResponse;
import com.throughline.taskmanagement.dto.response.CheckEmailResponse;
import com.throughline.taskmanagement.dto.response.PasswordResetRequestOutcome;
import com.throughline.taskmanagement.dto.response.PersonResponse;
import com.throughline.taskmanagement.enums.PasswordResetRequestStatus;
import com.throughline.taskmanagement.exception.ForbiddenActionException;
import com.throughline.taskmanagement.exception.InvalidAssignmentException;
import com.throughline.taskmanagement.exception.InvalidCredentialsException;
import com.throughline.taskmanagement.exception.ResourceNotFoundException;
import com.throughline.taskmanagement.mapper.PersonMapper;
import com.throughline.taskmanagement.model.PasswordResetRequest;
import com.throughline.taskmanagement.model.Person;
import com.throughline.taskmanagement.model.TotpRecoveryCode;
import com.throughline.taskmanagement.repository.PasswordResetRequestRepository;
import com.throughline.taskmanagement.repository.PersonRepository;
import com.throughline.taskmanagement.repository.TeamMemberRepository;
import com.throughline.taskmanagement.repository.TotpRecoveryCodeRepository;
import com.throughline.taskmanagement.security.JwtService;
import com.throughline.taskmanagement.security.LoginRateLimiter;
import com.throughline.taskmanagement.security.TotpService;
import com.throughline.taskmanagement.service.AuthService;
import com.throughline.taskmanagement.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final int PENDING_AUTH_TOKEN_TTL_MINUTES = 5;
    private static final int RECOVERY_CODE_COUNT = 10;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final PersonRepository personRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TotpRecoveryCodeRepository totpRecoveryCodeRepository;
    private final PasswordResetRequestRepository passwordResetRequestRepository;
    private final PersonMapper personMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginRateLimiter rateLimiter;
    private final TotpService totpService;
    private final NotificationService notificationService;

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

        if (person.getPassword() == null || !passwordEncoder.matches(request.password(), person.getPassword())) {
            // person.getPassword() == null covers a legacy row with no password ever set —
            // short-circuits before matches() so that never NPEs.
            rateLimiter.recordFailure(request.email());
            throw new InvalidCredentialsException("Invalid email or password.");
        }

        if (!person.isActive()) {
            throw new ForbiddenActionException("This account has been deactivated.");
        }

        rateLimiter.recordSuccess(request.email());
        return buildLoginOutcome(person);
    }

    @Override
    public AuthResponse confirmTotpSetup(TotpConfirmRequest request) {
        Person person = requirePendingAuthPerson(request.pendingAuthToken());
        rateLimiter.checkAllowed(person.getEmail());

        if (person.getTotpEnabledAt() != null) {
            throw new InvalidAssignmentException("This account has already completed TOTP setup.");
        }
        if (person.getTotpSecret() == null) {
            // Can't happen through the normal login flow (login generates the secret
            // before ever handing out this pendingAuthToken) — only reachable if this
            // token is stale/tampered with.
            throw new InvalidCredentialsException("This session has expired. Please log in again.");
        }
        if (!totpService.isCodeValid(person.getTotpSecret(), request.code())) {
            rateLimiter.recordFailure(person.getEmail());
            throw new InvalidCredentialsException("Incorrect code. Check the time on your phone and try again.");
        }

        rateLimiter.recordSuccess(person.getEmail());
        person.setTotpEnabledAt(LocalDateTime.now());
        clearPendingAuthToken(person);
        personRepository.save(person);

        List<String> recoveryCodes = generateRecoveryCodes(person);
        return authenticated(person, recoveryCodes);
    }

    @Override
    public AuthResponse verifyTotp(TotpVerifyRequest request) {
        Person person = requirePendingAuthPerson(request.pendingAuthToken());
        rateLimiter.checkAllowed(person.getEmail());

        if (person.getTotpEnabledAt() == null || person.getTotpSecret() == null) {
            throw new InvalidCredentialsException("This session has expired. Please log in again.");
        }

        if (totpService.isCodeValid(person.getTotpSecret(), request.code())) {
            rateLimiter.recordSuccess(person.getEmail());
            clearPendingAuthToken(person);
            personRepository.save(person);
            return authenticated(person, null);
        }

        // Not a valid live code — try it as a recovery code before giving up. A recovery
        // code lets them in this once, but also assumes their original device is gone: it
        // clears enrollment entirely, so their very next login starts fresh at setup.
        TotpRecoveryCode matchedCode = totpRecoveryCodeRepository.findByPersonIdAndUsedAtIsNull(person.getId())
                .stream()
                .filter(rc -> passwordEncoder.matches(request.code(), rc.getCodeHash()))
                .findFirst()
                .orElse(null);

        if (matchedCode == null) {
            rateLimiter.recordFailure(person.getEmail());
            throw new InvalidCredentialsException("Incorrect code.");
        }

        rateLimiter.recordSuccess(person.getEmail());
        matchedCode.setUsedAt(LocalDateTime.now());
        totpRecoveryCodeRepository.save(matchedCode);

        person.setTotpSecret(null);
        person.setTotpEnabledAt(null);
        clearPendingAuthToken(person);
        personRepository.save(person);

        return authenticated(person, null);
    }

    @Override
    public CheckEmailResponse checkEmailForPasswordReset(PasswordResetEmailRequest request) {
        // Deliberately NOT silent about whether the account exists — an internal, offline
        // tool gets more value from telling a real user their account genuinely isn't
        // found than from the enumeration-proof non-answer a public internet-facing
        // service would need. That tradeoff is exactly why this is rate-limited: without a
        // guess limit, a truthful yes/no answer turns this into a roster-scanning tool.
        rateLimiter.checkAllowed(request.email());

        boolean exists = personRepository.findByEmailIgnoreCase(request.email()).isPresent();
        if (exists) {
            rateLimiter.recordSuccess(request.email());
        } else {
            rateLimiter.recordFailure(request.email());
        }
        return new CheckEmailResponse(exists);
    }

    @Override
    public PasswordResetRequestOutcome createPasswordResetRequest(PasswordResetEmailRequest request) {
        Person person = personRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("Person not found"));

        boolean alreadyPending = passwordResetRequestRepository
                .findByPersonIdAndStatus(person.getId(), PasswordResetRequestStatus.PENDING)
                .isPresent();
        if (alreadyPending) {
            return new PasswordResetRequestOutcome(PasswordResetRequestOutcome.ALREADY_PENDING);
        }

        PasswordResetRequest resetRequest = new PasswordResetRequest();
        resetRequest.setPerson(person);
        passwordResetRequestRepository.save(resetRequest);

        notificationService.notifyPasswordResetRequestReceived(resetRequest);

        return new PasswordResetRequestOutcome(PasswordResetRequestOutcome.CREATED);
    }

    @Override
    public PersonResponse getCurrentPerson(String email) {
        Person person = personRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Person not found"));
        return personMapper.toResponse(person, teamMemberRepository.findByPersonId(person.getId()));
    }

    /** After identity + password succeeds, decides whether the login is actually done or
     *  still needs a TOTP step, and returns the correspondingly shaped AuthResponse either
     *  way. */
    private AuthResponse buildLoginOutcome(Person person) {
        if (!person.isTotpRequired()) {
            return authenticated(person, null);
        }

        if (person.getTotpEnabledAt() == null) {
            // Not yet enrolled. Reuse an already-generated-but-unconfirmed secret rather
            // than replacing it — a repeated login attempt mid-enrollment must show the
            // same QR the person may have already scanned, not a new one.
            if (person.getTotpSecret() == null) {
                person.setTotpSecret(totpService.generateSecret());
            }
            String pendingToken = issuePendingAuthToken(person);
            String qrCodeDataUri = totpService.buildQrCodeDataUri(person.getEmail(), person.getTotpSecret());
            return new AuthResponse(null, person.getId(), person.getFullName(), person.getEmail(), person.getRole(),
                    AuthResponse.STATUS_TOTP_SETUP_REQUIRED, pendingToken, person.getTotpSecret(), qrCodeDataUri, null);
        }

        String pendingToken = issuePendingAuthToken(person);
        return new AuthResponse(null, person.getId(), person.getFullName(), person.getEmail(), person.getRole(),
                AuthResponse.STATUS_TOTP_REQUIRED, pendingToken, null, null, null);
    }

    private AuthResponse authenticated(Person person, List<String> recoveryCodes) {
        String token = jwtService.generateToken(person.getEmail());
        return new AuthResponse(token, person.getId(), person.getFullName(), person.getEmail(), person.getRole(),
                AuthResponse.STATUS_AUTHENTICATED, null, null, null, recoveryCodes);
    }

    private String issuePendingAuthToken(Person person) {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        person.setPendingAuthToken(token);
        person.setPendingAuthTokenExpiresAt(LocalDateTime.now().plusMinutes(PENDING_AUTH_TOKEN_TTL_MINUTES));
        personRepository.save(person);
        return token;
    }

    private void clearPendingAuthToken(Person person) {
        person.setPendingAuthToken(null);
        person.setPendingAuthTokenExpiresAt(null);
    }

    private Person requirePendingAuthPerson(String pendingAuthToken) {
        Person person = personRepository.findByPendingAuthToken(pendingAuthToken)
                .orElseThrow(() -> new InvalidCredentialsException("This session has expired. Please log in again."));
        if (person.getPendingAuthTokenExpiresAt() == null
                || person.getPendingAuthTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidCredentialsException("This session has expired. Please log in again.");
        }
        return person;
    }

    /** Only ever called right as enrollment is confirmed — never regenerated afterward via
     *  this path, so a person can't accidentally invalidate their existing codes just by
     *  logging in again. A lost set is a Super Admin TOTP reset away (see
     *  PersonServiceImpl.resetTotp), which re-enrolls from scratch including a fresh batch. */
    private List<String> generateRecoveryCodes(Person person) {
        totpRecoveryCodeRepository.deleteByPersonId(person.getId());

        List<String> plainCodes = new ArrayList<>(RECOVERY_CODE_COUNT);
        for (int i = 0; i < RECOVERY_CODE_COUNT; i++) {
            String plain = String.format("%04d-%04d", SECURE_RANDOM.nextInt(10_000), SECURE_RANDOM.nextInt(10_000));
            plainCodes.add(plain);

            TotpRecoveryCode recoveryCode = new TotpRecoveryCode();
            recoveryCode.setPerson(person);
            recoveryCode.setCodeHash(passwordEncoder.encode(plain));
            totpRecoveryCodeRepository.save(recoveryCode);
        }
        return plainCodes;
    }
}
