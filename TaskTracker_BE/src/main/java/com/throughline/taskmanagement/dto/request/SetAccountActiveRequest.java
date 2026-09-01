package com.throughline.taskmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** changedById must be a Super Admin — enforced in the service, not just trusted here.
 *  reason is mandatory — deactivating or reactivating someone always goes on record with
 *  why. */
public record SetAccountActiveRequest(
    boolean active,
    @NotNull Long changedById,
    @NotBlank String reason
) {}
