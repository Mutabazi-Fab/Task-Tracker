package com.throughline.taskmanagement.enums;

public enum AssigneeType {
    INDIVIDUAL,
    TEAM,
    /** Executive-only, top-level (depth 0) tasks assigned to a whole Department rather
     *  than a Team or a Person — the Department's head Director then turns it into a real
     *  team-or-individual "implementation task" (a depth-1 child, created the same way as
     *  any other top-level task, see TaskServiceImpl.createSubtask). Never used below
     *  depth 0 — a subtask is always TEAM or INDIVIDUAL. */
    DEPARTMENT
}
