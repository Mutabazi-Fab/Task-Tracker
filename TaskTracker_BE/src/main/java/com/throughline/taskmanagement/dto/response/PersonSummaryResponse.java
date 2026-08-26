package com.throughline.taskmanagement.dto.response;

public record PersonSummaryResponse(
    String name,
    String role,
    Double averageProgress,
    long assignedCount,
    long completedCount
) {}
