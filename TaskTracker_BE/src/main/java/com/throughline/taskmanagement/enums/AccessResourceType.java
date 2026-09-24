package com.throughline.taskmanagement.enums;

/** What an access grant points at. RISK is reserved for the risk register, which doesn't exist yet
 *  (listed now so adding it later doesn't need a database constraint change). */
public enum AccessResourceType {
    TASK,
    INCIDENT,
    RISK
}
