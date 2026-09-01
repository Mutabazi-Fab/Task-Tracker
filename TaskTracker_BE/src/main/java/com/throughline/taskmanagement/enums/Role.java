package com.throughline.taskmanagement.enums;

/**
 * Global role, in ascending order of authority: MEMBER < DIRECTOR < SUPER_ADMIN.
 * "Team Leader" is deliberately NOT a value here — leadership is scoped per-team
 * (TeamMember.isLeader), so a person can lead one team and be a plain member of another.
 *
 * SUPER_ADMIN has every permission DIRECTOR has, plus a few exclusively its own (granting
 * DIRECTOR/SUPER_ADMIN itself, deactivating accounts, org-wide activity visibility beyond
 * a Director's own initiatives) — see {@link #isAtLeastDirector} and the
 * requireSuperAdmin-gated methods in PersonServiceImpl.
 */
public enum Role {
    DIRECTOR,
    MEMBER,
    SUPER_ADMIN;

    /**
     * True for DIRECTOR and SUPER_ADMIN — every "Director-only" check in the app should
     * call this rather than compare directly against Role.DIRECTOR, so Super Admin never
     * ends up unable to do something a Director can. Null-safe (a null role — an account
     * that predates auth entirely — is never "at least Director").
     */
    public static boolean isAtLeastDirector(Role role) {
        return role == DIRECTOR || role == SUPER_ADMIN;
    }
}
