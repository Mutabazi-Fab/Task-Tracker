package com.throughline.taskmanagement.dto.response;

import java.util.List;

public record TeamStatisticsResponse(
    Double averageProgress,
    long taskCount,
    long memberCount,
    long completedCount,
    List<MemberProgress> memberProgresses
) {
    public record MemberProgress(String name, Double averageProgress) {}
}
