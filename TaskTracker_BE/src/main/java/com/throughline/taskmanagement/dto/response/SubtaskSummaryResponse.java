package com.throughline.taskmanagement.dto.response;

import com.throughline.taskmanagement.enums.CreatedByRole;
import com.throughline.taskmanagement.enums.TaskStatus;

/** The Director's dashboard view of one subtask under a top-level task: who created it
 *  (Director or Team Leader), who it's assigned to, and its status/progress. */
public record SubtaskSummaryResponse(
    Long id,
    String taskCode,
    String title,
    String assigneeName,
    TaskStatus status,
    int progressPercentage,
    CreatedByRole createdByRole
) {}
