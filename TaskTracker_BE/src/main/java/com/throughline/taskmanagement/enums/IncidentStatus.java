package com.throughline.taskmanagement.enums;

/** Exactly the Excel register's "Status" dropdown, in the same 5 values — deliberately no
 *  extra states invented on top of it (e.g. no separate "Review" status: Risk/Compliance
 *  review is tracked by {@link IncidentReviewStatus} on its own two fields, same as the
 *  source workbook, and is enforced as a gate on closing rather than a status of its own). */
public enum IncidentStatus {
    OPEN,
    UNDER_INVESTIGATION,
    MONITORING,
    CLOSED,
    REJECTED_NOT_AN_INCIDENT
}
