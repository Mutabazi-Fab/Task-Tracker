package com.throughline.taskmanagement.dto.response;

/** Response for POST /auth/password-reset-requests. */
public record PasswordResetRequestOutcome(
    String status
) {
    public static final String CREATED = "CREATED";
    public static final String ALREADY_PENDING = "ALREADY_PENDING";
}
