package com.throughline.taskmanagement.dto.request;

import jakarta.validation.constraints.NotNull;

/** changedById must be a Super Admin — enforced in the service, not just trusted here. */
public record SetAccountActiveRequest(
    boolean active,
    @NotNull Long changedById,
    String reason
) {}
