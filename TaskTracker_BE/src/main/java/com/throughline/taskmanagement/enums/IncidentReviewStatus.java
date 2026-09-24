package com.throughline.taskmanagement.enums;

/** Risk Review / Compliance Review columns from the Excel register. Closing a
 *  CRITICAL/HIGH-severity or regulator-notifiable incident requires both to be COMPLETED —
 *  see IncidentServiceImpl.requireClosureReadiness. */
public enum IncidentReviewStatus {
    PENDING,
    COMPLETED,
    NOT_REQUIRED
}
