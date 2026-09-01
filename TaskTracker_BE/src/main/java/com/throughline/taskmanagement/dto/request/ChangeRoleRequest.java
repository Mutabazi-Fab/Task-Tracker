package com.throughline.taskmanagement.dto.request;

import com.throughline.taskmanagement.enums.Role;
import jakarta.validation.constraints.NotNull;

/** changedById must be a Super Admin — enforced in the service, not just trusted here. */
public record ChangeRoleRequest(
    @NotNull Role newRole,
    @NotNull Long changedById,
    String reason
) {}
