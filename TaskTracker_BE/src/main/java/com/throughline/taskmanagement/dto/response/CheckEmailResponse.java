package com.throughline.taskmanagement.dto.response;

/** Response for POST /auth/password-reset-requests/check. */
public record CheckEmailResponse(
    boolean exists
) {}
