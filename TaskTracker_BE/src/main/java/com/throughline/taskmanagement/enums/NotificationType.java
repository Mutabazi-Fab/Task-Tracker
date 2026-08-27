package com.throughline.taskmanagement.enums;

/**
 * Extensible on purpose: adding a future type (e.g. TASK_OVERDUE, SUBTASK_STALLED) never
 * needs a new column or table — every notification already carries a generic message and
 * relatedEntityId, so a new type is just a new constant here plus whatever service logic
 * creates it.
 */
public enum NotificationType {
    TEAM_MEMBER_ADDED,
    TEAM_MEMBER_REMOVED
}
