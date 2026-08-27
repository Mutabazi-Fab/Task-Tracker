package com.throughline.taskmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** changedById must be either the team's own leader or a Director — enforced in the service. */
public record AddTeamMemberRequest(
    @NotNull Long personId,
    @NotNull Long changedById,
    @NotBlank String reason
) {}
