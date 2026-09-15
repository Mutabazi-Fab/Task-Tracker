package com.throughline.taskmanagement.dto.response;

import com.throughline.taskmanagement.enums.DepartmentHealth;

/** One row of the Executive Dashboard's department roll-up — replaces the flat task-card
 *  list an Executive used to see, same N+1-per-department aggregation style as
 *  TeamLeaderboardResponse/PersonSummaryResponse (see DashboardServiceImpl.
 *  buildDepartmentHealth). id is included (unlike TeamLeaderboardResponse, which keys off
 *  name) so the frontend can link straight into DepartmentPage without a lookup. */
public record DepartmentHealthResponse(
    Long id,
    String name,
    String headDirectorName,
    Double averageProgress,
    long taskCount,
    long completedCount,
    long overdueCount,
    DepartmentHealth health
) {}
