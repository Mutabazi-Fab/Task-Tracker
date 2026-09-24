package com.throughline.taskmanagement.enums;

/** The lifecycle of one TaskDeadlineExtensionRequest row — set once at creation (PENDING), then exactly
 *  once more when a decision is made (APPROVED/REJECTED). */
public enum ExtensionRequestStatus {
    PENDING,
    APPROVED,
    REJECTED
}
