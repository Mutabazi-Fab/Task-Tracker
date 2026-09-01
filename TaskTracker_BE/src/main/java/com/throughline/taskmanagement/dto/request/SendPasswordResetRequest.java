package com.throughline.taskmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** changedById must be a Super Admin — enforced in the service, not just trusted here.
 *  reason is mandatory — same rule as every other admin-triggered change to someone's
 *  account (role, active status): no action goes on record without one. */
public record SendPasswordResetRequest(
    @NotNull Long changedById,
    @NotBlank String reason
) {}
