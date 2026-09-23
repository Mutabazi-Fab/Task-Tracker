package com.throughline.taskmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Body for POST /people/{id}/set-password. Super-Admin-only; changedById is re-derived
 *  from the caller's real login server-side, never trusted from the request (same pattern
 *  as every other admin action). reason is mandatory, same rule as changeRole/setActive/
 *  resetTotp — no action on someone's account goes on record without one. Sets the
 *  password only: never touches totpSecret/totpEnabledAt, so an existing TOTP enrollment
 *  survives a password change untouched (see PersonServiceImpl.setPasswordDirectly). */
public record SetPasswordRequest(
    @NotNull Long changedById,
    @NotBlank @Size(min = 8, message = "Password must be at least 8 characters") String newPassword,
    @NotBlank String reason
) {}
