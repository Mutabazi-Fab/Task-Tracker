package com.throughline.taskmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateTeamRequest(
    @NotBlank String name,
    Long teamLeaderId
) {}
