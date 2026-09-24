package com.throughline.taskmanagement.dto.response;

import com.throughline.taskmanagement.enums.Role;

import java.util.List;

/** What login/totp-confirm/totp-verify all return — status tells the caller which of three shapes this
 *  is, since only one of (token) vs (pendingAuthToken [+ totpSecret/totpQrCodeDataUri]) is ever
 *  populated: - AUTHENTICATED: token is set, the person is fully logged in. recoveryCodes is set too,
 *  exactly once, right when TOTP enrollment is confirmed for the first time — the only moment those
 *  codes are ever shown, so the frontend must display them prominently then. - TOTP_SETUP_REQUIRED:
 *  this account requires TOTP and has never completed enrollment. pendingAuthToken, totpSecret
 *  (manual-entry fallback), and totpQrCodeDataUri are set; token is null. pendingAuthToken must be
 *  submitted to POST /auth/totp/confirm. - TOTP_REQUIRED: this account is already enrolled; a code is
 *  needed to finish logging in. */
public record AuthResponse(
    String token,
    Long personId,
    String fullName,
    String email,
    Role role,
    String status,
    String pendingAuthToken,
    String totpSecret,
    String totpQrCodeDataUri,
    List<String> recoveryCodes
) {
    public static final String STATUS_AUTHENTICATED = "AUTHENTICATED";
    public static final String STATUS_TOTP_SETUP_REQUIRED = "TOTP_SETUP_REQUIRED";
    public static final String STATUS_TOTP_REQUIRED = "TOTP_REQUIRED";
}
