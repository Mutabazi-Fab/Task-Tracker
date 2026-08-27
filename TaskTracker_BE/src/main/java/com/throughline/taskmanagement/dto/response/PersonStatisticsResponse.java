package com.throughline.taskmanagement.dto.response;

import java.util.List;

public record PersonStatisticsResponse(
    Double averageProgress,
    long tasksAssigned,
    long tasksCompleted,
    long tasksOngoing,
    long tasksPending,
    long commentsLogged,
    long tasksHandedOff,
    boolean fullyCompleted,
    // Per-team breakdown — e.g. "50% avg on Auditing App team, 100% on Compliance team" —
    // alongside (not instead of) the org-wide totals above, which still matter for
    // whole-org views like the dashboard leaderboard.
    List<PersonTeamStatisticsResponse> teamBreakdown
) {}
