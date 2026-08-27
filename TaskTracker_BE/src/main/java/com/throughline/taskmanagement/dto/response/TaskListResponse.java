package com.throughline.taskmanagement.dto.response;

import com.throughline.taskmanagement.enums.AssigneeType;
import com.throughline.taskmanagement.enums.CreatedByRole;
import com.throughline.taskmanagement.enums.TaskStatus;
import java.time.LocalDate;
import java.util.List;

public record TaskListResponse(
    Long id,
    String taskCode,
    String title,
    String assigneeName,
    AssigneeType assigneeType,
    TaskStatus status,
    int progressPercentage,
    LocalDate dateAssigned,
    String assignedByName,
    int reassignmentCount,
    CommentResponse lastComment,
    Long parentTaskId,
    CreatedByRole createdByRole,
    // Empty for a subtask (subtasks can't nest). For a top-level task, lets a list view
    // (e.g. the Director's Dashboard) show who created each subtask and who it's assigned
    // to without a second call per row.
    List<SubtaskSummaryResponse> subtasks
) {}
