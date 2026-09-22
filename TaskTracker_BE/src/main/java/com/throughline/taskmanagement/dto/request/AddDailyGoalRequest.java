package com.throughline.taskmanagement.dto.request;

import jakarta.validation.constraints.NotNull;

/** taskId must already be one of the caller's own assigned tasks — enforced in
 *  PersonServiceImpl.addDailyGoal. */
public record AddDailyGoalRequest(
    @NotNull Long taskId
) {}
