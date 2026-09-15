package com.throughline.taskmanagement.enums;

/** A department's traffic-light status on the Executive Dashboard's roll-up, derived fresh
 *  on every read from its top-level tasks' overdue count and average progress
 *  (DashboardServiceImpl.buildDepartmentHealth) — never a stored column, same "derive, don't
 *  persist" approach as TaskStatus vs. progressPercentage.
 *  BEHIND: at least one overdue top-level task (a hard signal — missing a date always wins
 *  over a merely-low average).
 *  AT_RISK: nothing overdue yet, but average progress across top-level tasks is under 50%.
 *  ON_TRACK: nothing overdue and average progress is 50% or higher. */
public enum DepartmentHealth {
    ON_TRACK,
    AT_RISK,
    BEHIND
}
