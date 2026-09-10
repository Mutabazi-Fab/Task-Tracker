package com.throughline.taskmanagement.enums;

/**
 * Global role, in ascending order of authority: MEMBER < DIRECTOR < EXECUTIVE < SUPER_ADMIN.
 * "Team Leader" is deliberately NOT a value here — leadership is scoped per-team
 * (TeamMember.isLeader), so a person can lead one team and be a plain member of another.
 * Same reasoning for "Department Head" — that's Department.headDirector, not a Role value.
 *
 * EXECUTIVE sits just above DIRECTOR — everything a Director can do, plus creating a
 * top-level task assigned to a whole Department rather than a team or person directly (see
 * AssigneeType.DEPARTMENT). It is a distinct seat on the org chart, not a rename of
 * SUPER_ADMIN — Super Admin keeps its existing, separate meaning (system/people
 * governance: role changes, account activation, department administration).
 *
 * SUPER_ADMIN has every permission DIRECTOR and EXECUTIVE have, plus a few exclusively its
 * own (granting any role, deactivating accounts, department administration) — see
 * {@link #isAtLeastDirector}, {@link #isAtLeastExecutive}, and the requireSuperAdmin-gated
 * methods in PersonServiceImpl/DepartmentServiceImpl.
 */
public enum Role {
    DIRECTOR,
    EXECUTIVE,
    MEMBER,
    SUPER_ADMIN;

    /**
     * True for DIRECTOR, EXECUTIVE, and SUPER_ADMIN — every "Director-only" check in the
     * app should call this rather than compare directly against Role.DIRECTOR, so Executive
     * and Super Admin never end up unable to do something a Director can. Null-safe (a null
     * role — an account that predates auth entirely — is never "at least Director").
     */
    public static boolean isAtLeastDirector(Role role) {
        return role == DIRECTOR || role == EXECUTIVE || role == SUPER_ADMIN;
    }

    /**
     * True for EXECUTIVE and SUPER_ADMIN only — the handful of CEO-tier gates a Director
     * (even a Director heading a Department) doesn't pass: creating a task assigned
     * straight to a Department, setting a task's severity, and the org-wide executive
     * dashboard view.
     */
    public static boolean isAtLeastExecutive(Role role) {
        return role == EXECUTIVE || role == SUPER_ADMIN;
    }
}
