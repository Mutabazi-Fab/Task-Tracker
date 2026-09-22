package com.throughline.taskmanagement.dto.response;

import com.throughline.taskmanagement.enums.AssigneeType;
import com.throughline.taskmanagement.enums.CreatedByRole;
import com.throughline.taskmanagement.enums.TaskSeverity;
import com.throughline.taskmanagement.enums.TaskSource;
import com.throughline.taskmanagement.enums.TaskStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    // Null only for a task that predates this field.
    LocalDate deadline,
    TaskSource source,
    String sourceLabel,
    TaskSeverity severity,
    boolean pinned,
    String assignedByName,
    int reassignmentCount,
    CommentResponse lastComment,
    Long parentTaskId,
    // Null for a top-level task — set only for a real subtask, so a list view can tell
    // "assigned to one person directly" apart from "a subtask of a team's top-level task".
    String parentTaskCode,
    // For UI copy like "under {parentTaskTitle}" instead of the less legible task code.
    String parentTaskTitle,
    CreatedByRole createdByRole,
    // Empty for a leaf subtask. Lets a list view show who created each subtask and who
    // it's assigned to without a second call per row.
    List<SubtaskSummaryResponse> subtasks,
    // 0 top-level, 1 direct child, 2 grandchild (Department-rooted hierarchy only).
    int depth,
    // Backs the "New" badge on a task list row — the frontend compares this against "now"
    // itself rather than the backend precomputing a boolean, so the badge disappears on its
    // own as time passes without needing a fresh fetch to notice.
    LocalDateTime createdAt
) {}
