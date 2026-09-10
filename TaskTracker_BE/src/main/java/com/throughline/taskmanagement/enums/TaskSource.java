package com.throughline.taskmanagement.enums;

/** Who actually originated this task — settable by whoever creates it, at any depth, no
 *  role restriction (unlike severity, below). Paired with a free-text sourceLabel on the
 *  task itself (e.g. "Director Musoni", "GPO", "E&Y", "Board of Directors") — not a Person
 *  FK, since these are often external/organizational, not app users. */
public enum TaskSource {
    INITIATIVE,
    AUDITOR,
    REGULATOR,
    BOARD
}
