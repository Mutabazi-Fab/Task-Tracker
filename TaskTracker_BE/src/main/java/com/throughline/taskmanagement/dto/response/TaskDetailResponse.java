package com.throughline.taskmanagement.dto.response;

import com.throughline.taskmanagement.enums.AssigneeType;
import com.throughline.taskmanagement.enums.CreatedByRole;
import com.throughline.taskmanagement.enums.Role;
import com.throughline.taskmanagement.enums.TaskSeverity;
import com.throughline.taskmanagement.enums.TaskSource;
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
    // Department this task actually lives in, regardless of assigneeType — see
    // TaskServiceImpl.resolveTaskDepartment. Lets the frontend gate pin/reassign/delete UI
    // against "does the viewer head this department" without a second fetch.
    Long taskDepartmentId,
    // Team actually responsible for this task: same as assigneeId for a top-level
    // TEAM-assigned task, else its parent task's team. Lets the frontend decide "is the
    // viewer this task's team leader" without a second fetch for the parent.
    Long owningTeamId,
    TaskStatus status,
    int progressPercentage,
    LocalDate dateAssigned,
    // Null only for a task that predates this field. See deadlineExtensions below and
    // TaskService.requestDeadlineExtension/decideDeadlineExtension/extendDeadlineDirectly.
    LocalDate deadline,
    // Both null unless this task's creator chose to record where it originated.
    TaskSource source,
    String sourceLabel,
    // Null unless an Executive/Super Admin set it at creation.
    TaskSeverity severity,
    // Manual toggle, not derived from severity — see TaskService.setPinned.
    boolean pinned,
    String assignedByName,
    Long assignedById,
    // Shown next to assignedByName so it's clear at a glance which tier this task came
    // from (e.g. "Director" vs "Executive").
    Role assignedByRole,
    // Who actually decides a deadline extension — a Director-or-above, always, even when
    // assignedById is a mere Team Leader. The frontend should gate deadline-decision UI
    // off THIS field, not assignedById. See TaskServiceImpl.resolveDeadlineDecider.
    String deadlineDeciderName,
    Long deadlineDeciderId,
    Long parentTaskId,
    String parentTaskCode,
    // Lets the frontend show a real "back to {title}" breadcrumb instead of a bare code.
    String parentTaskTitle,
    CreatedByRole createdByRole,
    // 0 top-level, 1 direct child, 2 grandchild (Department-rooted only). Lets the
    // frontend decide whether "Add subtask" should be offered and which form shape to show.
    int depth,
    List<SubtaskSummaryResponse> subtasks,
    List<CommentResponse> comments,
    List<ReassignmentResponse> reassignments,
    List<DeadlineExtensionResponse> deadlineExtensions,
    List<DocumentResponse> documents,
    List<TaskTimelineResponse> progressTimeline,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
