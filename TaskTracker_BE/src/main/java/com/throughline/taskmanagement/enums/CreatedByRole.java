package com.throughline.taskmanagement.enums;

/** Records who structured a piece of work — a top-level task is always DIRECTOR;
 *  a subtask is DIRECTOR (Director created it directly, bypassing the Team Leader)
 *  or TEAM_LEADER (the normal path). This is audit information, not an authorization
 *  check — the check itself happens in TaskServiceImpl at creation time. */
public enum CreatedByRole {
    DIRECTOR,
    TEAM_LEADER
}
