package com.throughline.taskmanagement.enums;

/** Executive-only, settable at creation only, at any depth (see TaskServiceImpl — a Director creating a
 *  depth-1 task under a Department root still can't set this, even when the Department task above it is
 *  CRITICAL). */
public enum TaskSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}
