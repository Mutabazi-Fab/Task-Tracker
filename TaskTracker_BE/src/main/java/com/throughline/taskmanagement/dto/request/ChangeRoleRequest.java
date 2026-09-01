package com.throughline.taskmanagement.dto.request;

import com.throughline.taskmanagement.enums.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** changedById must be a Super Admin — enforced in the service, not just trusted here.
 *  reason is mandatory — same rule as team membership changes: no promotion or demotion
 *  goes on record without one. */
public record ChangeRoleRequest(
    @NotNull Role newRole,
    @NotNull Long changedById,
    @NotBlank String reason
) {}
