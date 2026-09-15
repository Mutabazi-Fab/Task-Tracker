package com.throughline.taskmanagement.enums;

/**
 * Extensible on purpose: adding a future type (e.g. TASK_OVERDUE, SUBTASK_STALLED) never
 * needs a new column or table — every notification already carries a generic message and
 * relatedEntityId, so a new type is just a new constant here plus whatever service logic
 * creates it.
 */
public enum NotificationType {
    TEAM_MEMBER_ADDED,
    TEAM_MEMBER_REMOVED,
    TEAM_LEADER_CHANGED,
    ROLE_CHANGED,
    ACCOUNT_STATUS_CHANGED,
    PASSWORD_RESET_REQUESTED,
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
    TASK_DELETED
}
