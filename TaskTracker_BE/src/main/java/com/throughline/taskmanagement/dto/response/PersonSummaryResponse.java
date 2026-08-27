package com.throughline.taskmanagement.dto.response;

public record PersonSummaryResponse(
    String name,
    String jobTitle,
    Double averageProgress,
    long assignedCount,
    long completedCount
) {}
