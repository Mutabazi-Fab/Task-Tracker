package com.throughline.taskmanagement.dto.request;

import jakarta.validation.constraints.NotNull;

/** Reassigning a team's leader is a Director-only action — changedById is checked in the service. */
public record SetTeamLeaderRequest(
    @NotNull Long changedById
) {}
