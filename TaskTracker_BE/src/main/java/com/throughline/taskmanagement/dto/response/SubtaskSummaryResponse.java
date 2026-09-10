package com.throughline.taskmanagement.dto.response;

import com.throughline.taskmanagement.enums.AssigneeType;
import com.throughline.taskmanagement.enums.CreatedByRole;
import com.throughline.taskmanagement.enums.TaskStatus;

/** The Director's dashboard view of one subtask under a top-level task: who created it
 *  (Director or Team Leader), who it's assigned to, and its status/progress. assigneeType
 *  lets the frontend tell a depth-1 TEAM-assigned "implementation task" (which can itself
 *  be broken into further subtasks) apart from an ordinary INDIVIDUAL leaf subtask
 *  (which never can) without a second fetch. */
public record SubtaskSummaryResponse(
    Long id,
    String taskCode,
    String title,
    String assigneeName,
    AssigneeType assigneeType,
    TaskStatus status,
    int progressPercentage,
    CreatedByRole createdByRole
) {}
