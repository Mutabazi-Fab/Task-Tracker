package com.throughline.taskmanagement.dto.response;

public record PersonStatisticsResponse(
    Double averageProgress,
    long tasksAssigned,
    long tasksCompleted,
    long tasksOngoing,
    long tasksPending,
    long commentsLogged,
    long tasksHandedOff,
    boolean fullyCompleted
) {}
