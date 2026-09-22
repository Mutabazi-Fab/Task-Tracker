package com.throughline.taskmanagement.enums;

/**
 * Global role, in ascending order of authority: MEMBER < DIRECTOR < EXECUTIVE < SUPER_ADMIN.
 * "Team Leader" and "Department Head" are deliberately NOT values here — those are scoped
 * (TeamMember.isLeader, Department.headDirector), so one person can hold either on some
 * teams/departments and not others.
 *
 * EXECUTIVE sits just above DIRECTOR: everything a Director can do, plus creating a
 * top-level task assigned to a whole Department (see AssigneeType.DEPARTMENT). SUPER_ADMIN
 * has everything DIRECTOR and EXECUTIVE have, plus system/people governance (role changes,
 * account activation, department administration) — see {@link #isAtLeastDirector},
 * {@link #isAtLeastExecutive}.
 */
public enum Role {
    DIRECTOR,
    EXECUTIVE,
    MEMBER,
    SUPER_ADMIN;

    /** True for DIRECTOR, EXECUTIVE, and SUPER_ADMIN — use this rather than comparing
     *  directly against Role.DIRECTOR. Null-safe. */
    public static boolean isAtLeastDirector(Role role) {
        return role == DIRECTOR || role == EXECUTIVE || role == SUPER_ADMIN;
    }

    /** True for EXECUTIVE and SUPER_ADMIN only — the CEO-tier gates a Director doesn't
     *  pass: creating a Department-assigned task, setting severity, the executive dashboard. */
    public static boolean isAtLeastExecutive(Role role) {
        return role == EXECUTIVE || role == SUPER_ADMIN;
    }
}
