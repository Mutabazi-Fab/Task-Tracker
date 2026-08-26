package com.throughline.taskmanagement.dto.response;

public record TeamLeaderboardResponse(
    String name,
    String leaderName,
    Double averageProgress,
    long taskCount,
    long completedCount
) {}
