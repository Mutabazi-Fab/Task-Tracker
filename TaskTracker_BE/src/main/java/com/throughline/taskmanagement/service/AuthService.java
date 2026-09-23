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

    /** Checks identity + password same as always. If the account requires TOTP, this does
     *  NOT return a real session token — see AuthResponse's status field: the caller gets
     *  routed into TOTP enrollment or a TOTP challenge instead, and only confirmTotpSetup/
     *  verifyTotp below ever hand out the real token in that case. */
    AuthResponse login(LoginRequest request);

    /** Completes TOTP enrollment for an account whose pendingAuthToken (from login) shows
     *  it still needs one. Generates and returns a batch of recovery codes exactly once,
     *  right here — this is the only response that will ever contain them, so the
     *  frontend must show them prominently. */
    AuthResponse confirmTotpSetup(TotpConfirmRequest request);

    /** Completes a login for an already-enrolled account: checks a live TOTP code, or an
     *  unused recovery code as a fallback. Consuming a recovery code also clears the
     *  account's TOTP enrollment, so its very next login goes through setup again — the
     *  assumption being that using a recovery code means the original device is gone. */
    AuthResponse verifyTotp(TotpVerifyRequest request);

    /** Whether an account exists for this email — deliberately NOT silent, unlike the old
     *  mailed-code forgot-password flow: this app is internal/offline, and the product
     *  decision here is that telling a real user their account genuinely doesn't exist is
     *  better UX than a generic non-answer. That tradeoff is exactly why this is
     *  rate-limited (5 checks per email per 15 minutes, via LoginRateLimiter) — without a
     *  guess limit, a real "yes/no" answer turns this into a roster-enumeration endpoint. */
    CheckEmailResponse checkEmailForPasswordReset(PasswordResetEmailRequest request);

    /** Creates a PasswordResetRequest for this email and notifies every Super Admin —
     *  only reachable after checkEmailForPasswordReset already confirmed the account
     *  exists and the person explicitly confirmed they want to proceed. Returns
     *  ALREADY_PENDING instead of creating a duplicate if this person already has one
     *  outstanding, so repeated submissions can't spam every Super Admin with duplicate
     *  notifications. */
    PasswordResetRequestOutcome createPasswordResetRequest(PasswordResetEmailRequest request);

    PersonResponse getCurrentPerson(String email);
}
