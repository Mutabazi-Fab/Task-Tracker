package com.throughline.taskmanagement.dto.response;

/** The four top-of-page tiles on the Executive Dashboard. orgOnTrackPercentage is the share
 *  of departments (not tasks) whose derived DepartmentHealth is ON_TRACK — deliberately the
 *  same roll-up that backs the department-health table below it, so the tile and the table
 *  can never disagree. overdueCount is top-level-task-scoped for the same reason — it's
 *  meant to sum to the department table's own overdueCount column. criticalPendingCount is
 *  NOT depth-scoped — it mirrors what getExecutiveTasks already treats as "critical": any
 *  CRITICAL task org-wide regardless of depth. pendingDecisionsCount is exactly
 *  getPendingExtensionRequests(viewerId).size() — the same list the pending-decisions panel
 *  below it renders, so that pair can't disagree either. */
public record ExecutiveKpiResponse(
    Double orgOnTrackPercentage,
    long overdueCount,
    long criticalPendingCount,
    long pendingDecisionsCount
) {}
