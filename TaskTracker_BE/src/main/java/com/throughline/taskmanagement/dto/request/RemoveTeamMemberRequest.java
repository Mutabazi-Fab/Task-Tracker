package com.throughline.taskmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RemoveTeamMemberRequest(
    @NotNull Long changedById,
    @NotBlank String reason
) {}
