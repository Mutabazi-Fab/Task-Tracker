package com.throughline.taskmanagement.dto.response;

/** Response for POST /auth/password-reset-requests/check. Rate-limited (see
 *  AuthServiceImpl.checkEmailForPasswordReset) — 5 checks per email per 15-minute window,
 *  same LoginRateLimiter this app already uses for login/TOTP guessing, reused here
 *  against exactly the kind of scanning this endpoint makes possible (trying many emails
 *  to see which ones exist). */
public record CheckEmailResponse(
    boolean exists
) {}
