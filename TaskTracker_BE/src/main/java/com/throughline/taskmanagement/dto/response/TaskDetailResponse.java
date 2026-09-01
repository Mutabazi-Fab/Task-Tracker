package com.throughline.taskmanagement.dto.response;

import com.throughline.taskmanagement.enums.AssigneeType;
import com.throughline.taskmanagement.enums.CreatedByRole;
import com.throughline.taskmanagement.enums.TaskStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record TaskDetailResponse(
    Long id,
    String taskCode,
    String title,
    String description,
    String assigneeName,
    Long assigneeId,
    AssigneeType assigneeType,
    // The team actually responsible for this task, regardless of assigneeType: for a
    // top-level (TEAM-assigned) task, same as assigneeId; for a subtask (always
    // INDIVIDUAL-assigned), its parent task's team. Lets the frontend decide "is the
    // viewer this task's team leader" (who, along with a Director/Super Admin, is allowed
    // to reassign it) without a second fetch for the parent.
    Long owningTeamId,
    TaskStatus status,
    int progressPercentage,
    LocalDate dateAssigned,
    String assignedByName,
    Long assignedById,
    Long parentTaskId,
    String parentTaskCode,
    CreatedByRole createdByRole,
    List<SubtaskSummaryResponse> subtasks,
    List<CommentResponse> comments,
    List<ReassignmentResponse> reassignments,
    List<TaskTimelineResponse> progressTimeline,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
