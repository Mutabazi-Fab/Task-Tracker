package com.throughline.taskmanagement.enums;

/** A department's traffic-light status on the Executive Dashboard's roll-up, derived fresh on every
 *  read from its top-level tasks' overdue count and average progress
 *  (DashboardServiceImpl.buildDepartmentHealth) — never a stored column, same "derive, don't persist"
 *  approach as TaskStatus vs. progressPercentage. */
public enum DepartmentHealth {
    ON_TRACK,
    AT_RISK,
    BEHIND
}
