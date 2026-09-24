package com.throughline.taskmanagement.enums;

/** Records who structured a piece of work — a top-level task is always DIRECTOR; a subtask is DIRECTOR
 *  (Director created it directly, bypassing the Team Leader) or TEAM_LEADER (the normal path). */
public enum CreatedByRole {
    DIRECTOR,
    TEAM_LEADER
}
