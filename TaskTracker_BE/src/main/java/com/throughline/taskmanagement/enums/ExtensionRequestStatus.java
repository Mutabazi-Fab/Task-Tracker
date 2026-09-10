package com.throughline.taskmanagement.enums;

/** The lifecycle of one TaskDeadlineExtensionRequest row — set once at creation
 *  (PENDING), then exactly once more when a decision is made (APPROVED/REJECTED). A
 *  direct extension (TaskService.extendDeadlineDirectly) is logged as APPROVED from the
 *  start, self-decided by whoever extended it — the request/decide dance is skipped, but
 *  the audit trail still isn't. */
public enum ExtensionRequestStatus {
    PENDING,
    APPROVED,
    REJECTED
}
