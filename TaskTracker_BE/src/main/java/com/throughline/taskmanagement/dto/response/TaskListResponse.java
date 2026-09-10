package com.throughline.taskmanagement.dto.response;

import com.throughline.taskmanagement.enums.AssigneeType;
import com.throughline.taskmanagement.enums.CreatedByRole;
import com.throughline.taskmanagement.enums.TaskSeverity;
import com.throughline.taskmanagement.enums.TaskSource;
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
    // Null for a top-level task (team- or individually-assigned) — set only for a real
    // subtask, so a list view can tell "assigned to one person directly" apart from
    // "a subtask of some team's top-level task", which otherwise both show the identical
    // assigneeType INDIVIDUAL with nothing else distinguishing them.
    String parentTaskCode,
    CreatedByRole createdByRole,
    // Empty for a leaf subtask (can't nest further). For a top-level task, or a depth-1
    // TEAM-assigned implementation task, lets a list view (e.g. the Director's Dashboard)
    // show who created each subtask and who it's assigned to without a second call per row.
    List<SubtaskSummaryResponse> subtasks,
    // 0 for a real top-level task (plain or Department-assigned), 1 for a direct child
    // (an ordinary subtask, or a Department's implementation task), 2 for a grandchild
    // (only possible under a Department-rooted hierarchy).
    int depth
) {}
