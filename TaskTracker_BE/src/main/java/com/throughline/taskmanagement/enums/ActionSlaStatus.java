package com.throughline.taskmanagement.enums;

/** Mirrors the Excel register's "Action SLA" formula, computed fresh on every read (never
 *  persisted — it depends on TODAY()) by IncidentMapper. Adds CLOSED_LATE, which the source
 *  formula never had: closing an incident after its own target date used to just show
 *  "Closed" with no signal that it breached SLA (see IncidentMapper.computeActionSla). */
public enum ActionSlaStatus {
    ON_TRACK,
    DUE_SOON,
    OVERDUE,
    NO_DUE_DATE,
    CLOSED,
    CLOSED_LATE
}
