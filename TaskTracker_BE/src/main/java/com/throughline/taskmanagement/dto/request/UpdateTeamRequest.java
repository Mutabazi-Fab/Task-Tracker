package com.throughline.taskmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;

/** Rename only — membership and leadership are managed through their own dedicated endpoints. */
public record UpdateTeamRequest(
    @NotBlank String name
) {}
