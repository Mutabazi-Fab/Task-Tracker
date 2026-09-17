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
    // The department this task actually lives in right now, regardless of assigneeType —
    // its own assignedDepartment for a DEPARTMENT task, its team's department for a TEAM
    // task, its assignee's own department for an INDIVIDUAL one (see TaskServiceImpl.
    // resolveTaskDepartment, mirrored here). Lets the frontend gate pin/reassign/delete UI
    // against "does the viewer head this department" (or, for a plain Director, just
    // compare against their own departmentId — every current Director's own department
    // membership already matches their headship) without a second fetch.
    Long taskDepartmentId,
    // The team actually responsible for this task, regardless of assigneeType: for a
    // top-level (TEAM-assigned) task, same as assigneeId; for a subtask (always
    // INDIVIDUAL-assigned), its parent task's team. Lets the frontend decide "is the
    // viewer this task's team leader" (who, along with a Director/Super Admin, is allowed
    // to reassign it) without a second fetch for the parent.
    Long owningTeamId,
    TaskStatus status,
    int progressPercentage,
    LocalDate dateAssigned,
    // Null only for a task that predates this field. Extended directly by whoever set it,
    // or via the request/approve workflow — see deadlineExtensions below and
    // TaskService.requestDeadlineExtension/decideDeadlineExtension/extendDeadlineDirectly.
    LocalDate deadline,
    // Both null unless this task's creator chose to record where it originated.
    TaskSource source,
    String sourceLabel,
    // Null unless an Executive/Super Admin set it at creation — see TaskServiceImpl.
    TaskSeverity severity,
    // A manual, independently-editable toggle — see TaskService.setPinned. Not derived
    // from severity; a CRITICAL task can be freely un-pinned once it's on track.
    boolean pinned,
    String assignedByName,
    Long assignedById,
    // Whoever set this task's deadline/scope — a plain Director, an Executive/CEO, or a
    // Super Admin — shown on the task detail page next to their name so it's clear at a
    // glance which tier this task actually came from (e.g. "Théogène Habimana · Director"
    // vs "Fabiola Ikirezi · Executive"), not just who, without a second lookup.
    Role assignedByRole,
    // Who actually decides a deadline extension on this task — a Director-or-above,
    // always, even when assignedById is a mere Team Leader (who can create a leaf subtask
    // — see TaskServiceImpl.createLeafSubtask — but has no authority over its deadline).
    // Usually the same as assignedById/assignedByName above, but not always; the frontend
    // should gate deadline-decision UI off THIS field, not assignedById. See
    // TaskServiceImpl.resolveDeadlineDecider.
    String deadlineDeciderName,
    Long deadlineDeciderId,
    Long parentTaskId,
    String parentTaskCode,
    CreatedByRole createdByRole,
    // 0 for a real top-level task (plain or Department-assigned), 1 for a direct child, 2
    // for a grandchild (only possible under a Department-rooted hierarchy). Lets the
    // frontend decide whether "Add subtask" should even be offered (rejected server-side
    // past depth 2 regardless) and which shape of form to show (team-or-individual for a
    // Department's implementation task, individual-only for an ordinary subtask).
    int depth,
    List<SubtaskSummaryResponse> subtasks,
    List<CommentResponse> comments,
    List<ReassignmentResponse> reassignments,
    List<DeadlineExtensionResponse> deadlineExtensions,
    List<TaskTimelineResponse> progressTimeline,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
