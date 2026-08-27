package com.throughline.taskmanagement.enums;

/**
 * Global role. "Team Leader" is deliberately NOT a value here — leadership
 * is scoped per-team (TeamMember.isLeader), so a person can lead one team
 * and be a plain member of another. This enum only distinguishes the
 * Director (who creates top-level tasks and teams) from everyone else.
 */
public enum Role {
    DIRECTOR,
    MEMBER
}
