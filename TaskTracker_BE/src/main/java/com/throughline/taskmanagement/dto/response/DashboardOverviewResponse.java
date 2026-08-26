package com.throughline.taskmanagement.dto.response;

public record DashboardOverviewResponse(
    Double orgAverageProgress,
    long totalTasks,
    long completedCount,
    long ongoingCount,
    long pendingCount
) {}
