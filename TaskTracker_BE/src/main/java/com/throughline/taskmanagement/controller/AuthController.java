package com.throughline.taskmanagement.controller;

import com.throughline.taskmanagement.dto.request.LoginRequest;
import com.throughline.taskmanagement.dto.request.PasswordResetEmailRequest;
import com.throughline.taskmanagement.dto.request.TotpConfirmRequest;
import com.throughline.taskmanagement.dto.request.TotpVerifyRequest;
import com.throughline.taskmanagement.dto.response.AuthResponse;
import com.throughline.taskmanagement.dto.response.CheckEmailResponse;
import com.throughline.taskmanagement.dto.response.PasswordResetRequestOutcome;
import com.throughline.taskmanagement.dto.response.PersonResponse;
import com.throughline.taskmanagement.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /** Not gated by the normal Bearer-token auth — pendingAuthToken (from login) is the
     *  credential here, checked inside the service, since there's no real session yet. */
    @PostMapping("/totp/confirm")
    public ResponseEntity<AuthResponse> confirmTotpSetup(@Valid @RequestBody TotpConfirmRequest request) {
        return ResponseEntity.ok(authService.confirmTotpSetup(request));
    }

    /** Same pendingAuthToken-as-credential shape as confirmTotpSetup above. */
    @PostMapping("/totp/verify")
    public ResponseEntity<AuthResponse> verifyTotp(@Valid @RequestBody TotpVerifyRequest request) {
        return ResponseEntity.ok(authService.verifyTotp(request));
    }

    /** Public, unauthenticated, rate-limited (5 per email per 15 minutes) — see
     *  AuthService.checkEmailForPasswordReset for why this deliberately tells the truth
     *  about whether the account exists, instead of the old mailed-code flow's silent
     *  non-answer. */
    @PostMapping("/password-reset-requests/check")
    public ResponseEntity<CheckEmailResponse> checkEmailForPasswordReset(@Valid @RequestBody PasswordResetEmailRequest request) {
        return ResponseEntity.ok(authService.checkEmailForPasswordReset(request));
    }

    /** Public, unauthenticated — only reachable after the frontend already called the
     *  check endpoint above and the person explicitly confirmed they want to proceed. */
    @PostMapping("/password-reset-requests")
    public ResponseEntity<PasswordResetRequestOutcome> createPasswordResetRequest(@Valid @RequestBody PasswordResetEmailRequest request) {
        return ResponseEntity.ok(authService.createPasswordResetRequest(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        // JWT is stateless — there's nothing to invalidate server-side. "Logging out" is the
        // client discarding its token. This endpoint exists for API completeness/symmetry.
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<PersonResponse> me(Authentication authentication) {
        return ResponseEntity.ok(authService.getCurrentPerson(authentication.getName()));
    }
}
