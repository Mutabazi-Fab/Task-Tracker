package com.throughline.taskmanagement.enums;

/** Executive-only, settable at creation only, at any depth (see TaskServiceImpl — a
 *  Director creating a depth-1 task under a Department root still can't set this, even
 *  when the Department task above it is CRITICAL). CRITICAL sets Task.pinned = true as a
 *  one-time default at creation — pinning itself stays a separate, independently-editable
 *  toggle afterward (see Task.pinned / TaskService.setPinned), never re-enforced from here. */
public enum TaskSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}
