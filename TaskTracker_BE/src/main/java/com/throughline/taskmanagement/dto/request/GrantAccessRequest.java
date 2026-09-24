package com.throughline.taskmanagement.dto.request;

import com.throughline.taskmanagement.enums.AccessResourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Gives one person access to one task or incident. Executive/Super Admin only. grantedById is
 *  overwritten by the controller with the caller's own JWT-resolved identity. */
public record GrantAccessRequest(
    @NotNull AccessResourceType resourceType,
    @NotNull Long resourceId,
    @NotNull Long granteeId,
    @NotBlank String reason,
    @NotNull Long grantedById
) {}
