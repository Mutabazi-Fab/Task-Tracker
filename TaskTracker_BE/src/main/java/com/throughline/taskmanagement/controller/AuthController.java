package com.throughline.taskmanagement.controller;

import com.throughline.taskmanagement.dto.request.LoginRequest;
import com.throughline.taskmanagement.dto.request.ResendOtpRequest;
import com.throughline.taskmanagement.dto.request.SignupRequest;
import com.throughline.taskmanagement.dto.request.VerifyEmailRequest;
import com.throughline.taskmanagement.dto.response.AuthResponse;
import com.throughline.taskmanagement.dto.response.PersonResponse;
import com.throughline.taskmanagement.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

    /** May return a null token if this email still needs OTP verification (see
     *  AuthResponse.emailVerified) — the frontend routes to "verify your email" in that
     *  case rather than treating it as a failure. */
    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
        return new ResponseEntity<>(authService.signup(request), HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<AuthResponse> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        return ResponseEntity.ok(authService.verifyEmail(request));
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<Void> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        authService.resendOtp(request);
        return ResponseEntity.ok().build();
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
