package com.throughline.taskmanagement.dto.request;

import jakarta.validation.constraints.NotNull;

/** Body for POST /people/{id}/password-reset-request/dismiss. */
public record DismissPasswordResetRequestRequest(
    @NotNull Long changedById
) {}
