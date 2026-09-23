package com.throughline.taskmanagement.dto.response;

/** Response for POST /auth/password-reset-requests. ALREADY_PENDING lets the frontend
 *  say "You already have a pending request awaiting the Super Admin" instead of implying
 *  a fresh one just went out — this app tells the truth here rather than showing an
 *  identical message regardless of what actually happened. */
public record PasswordResetRequestOutcome(
    String status
) {
    public static final String CREATED = "CREATED";
    public static final String ALREADY_PENDING = "ALREADY_PENDING";
}
