package com.throughline.taskmanagement.service;

import com.throughline.taskmanagement.dto.request.LoginRequest;
import com.throughline.taskmanagement.dto.request.PasswordResetEmailRequest;
import com.throughline.taskmanagement.dto.request.TotpConfirmRequest;
import com.throughline.taskmanagement.dto.request.TotpVerifyRequest;
import com.throughline.taskmanagement.dto.response.AuthResponse;
import com.throughline.taskmanagement.dto.response.CheckEmailResponse;
import com.throughline.taskmanagement.dto.response.PasswordResetRequestOutcome;
import com.throughline.taskmanagement.dto.response.PersonResponse;

public interface AuthService {

    /** Checks identity + password same as always. */
    AuthResponse login(LoginRequest request);

    /** Completes TOTP enrollment for an account whose pendingAuthToken (from login) shows it still needs
     *  one. */
    AuthResponse confirmTotpSetup(TotpConfirmRequest request);

    /** Completes a login for an already-enrolled account: checks a live TOTP code, or an unused recovery
     *  code as a fallback. */
    AuthResponse verifyTotp(TotpVerifyRequest request);

    /** Whether an account exists for this email — deliberately NOT silent, unlike the old mailed-code
     *  forgot-password flow: this app is internal/offline, and the product decision here is that telling a
     *  real user their account genuinely doesn't exist is better UX than a generic non-answer. */
    CheckEmailResponse checkEmailForPasswordReset(PasswordResetEmailRequest request);

    /** Creates a PasswordResetRequest for this email and notifies every Super Admin — only reachable after
     *  checkEmailForPasswordReset already confirmed the account exists and the person explicitly confirmed
     *  they want to proceed. */
    PasswordResetRequestOutcome createPasswordResetRequest(PasswordResetEmailRequest request);

    PersonResponse getCurrentPerson(String email);
}
