package com.throughline.taskmanagement.dto.response;

/** One person's stats scoped to a single team they belong to — e.g. "50% avg on Auditing
 *  App team, 100% on Compliance team" — rather than one blended number across every team
 *  they're on. A subtask counts toward a team here via its parent (top-level) task's
 *  assignedTeam. */
public record PersonTeamStatisticsResponse(
    Long teamId,
    String teamName,
    Double averageProgress,
    long tasksAssigned,
    long tasksCompleted
) {}
