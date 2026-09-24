package com.throughline.taskmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Body for POST /people/{id}/set-password. */
public record SetPasswordRequest(
    @NotNull Long changedById,
    @NotBlank @Size(min = 8, message = "Password must be at least 8 characters") String newPassword,
    @NotBlank String reason
) {}
