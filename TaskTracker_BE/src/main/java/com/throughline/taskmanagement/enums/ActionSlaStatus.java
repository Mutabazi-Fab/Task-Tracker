package com.throughline.taskmanagement.enums;

/** Mirrors the Excel register's "Action SLA" formula, computed fresh on every read (never persisted —
 *  it depends on TODAY()) by IncidentMapper. */
public enum ActionSlaStatus {
    ON_TRACK,
    DUE_SOON,
    OVERDUE,
    NO_DUE_DATE,
    CLOSED,
    CLOSED_LATE
}
