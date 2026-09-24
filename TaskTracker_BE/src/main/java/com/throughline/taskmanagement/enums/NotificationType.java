package com.throughline.taskmanagement.enums;

/** Extensible on purpose: adding a future type (e.g. */
public enum NotificationType {
    TEAM_MEMBER_ADDED,
    TEAM_MEMBER_REMOVED,
    TEAM_LEADER_CHANGED,
    ROLE_CHANGED,
    ACCOUNT_STATUS_CHANGED,
    // Broadcast to every Super Admin — the "Forgot Password?" flow's admin-facing side
    // (AuthServiceImpl.createPasswordResetRequest / PersonServiceImpl.setPasswordDirectly).
    PASSWORD_RESET_REQUEST_RECEIVED,
    TOTP_RESET,
    TASK_STALLED,
    TASK_ASSIGNED,
    SUBTASK_ASSIGNED,
    TASK_REASSIGNED,
    SUBTASK_REASSIGNED,
    DEADLINE_EXTENSION_REQUESTED,
    DEADLINE_EXTENSION_APPROVED,
    DEADLINE_EXTENSION_REJECTED,
    DEADLINE_EXTENDED,
    DISCUSSION_COMMENT_POSTED,
    DISCUSSION_REPLY_POSTED,
    // Broadcast to every Director-or-above except whoever did it — back the sidebar's
    // per-section "new activity" badges (Teams/Departments/Activity), see
    // NotificationService.getUnreadCountsByType and Sidebar's own badge wiring.
    TEAM_CREATED,
    DEPARTMENT_CREATED,
    TASK_DELETED,
    // Broadcast to every Director-or-above except the reporter, same pattern as
    // TEAM_CREATED/DEPARTMENT_CREATED — see NotificationServiceImpl.notifyIncidentReported.
    INCIDENT_REPORTED,
    // Direct to the newly-assigned Action Owner — see notifyIncidentActionOwnerAssigned.
    INCIDENT_ACTION_OWNER_ASSIGNED
}
