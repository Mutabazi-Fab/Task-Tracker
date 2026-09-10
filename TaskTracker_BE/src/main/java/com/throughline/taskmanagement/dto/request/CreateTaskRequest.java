package com.throughline.taskmanagement.dto.request;

import com.throughline.taskmanagement.enums.TaskSeverity;
import com.throughline.taskmanagement.enums.TaskSource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** Creates a TOP-LEVEL (depth 0) task — Director-or-above. Assigned to exactly one of a
 *  team, a single individual, or (Executive/Super Admin only) a whole Department: exactly
 *  one of assignedTeamId/assignedPersonId/assignedDepartmentId must be set (the service
 *  rejects zero or more than one). A team-assigned top-level task later gets broken into
 *  subtasks (see CreateSubtaskRequest, POST /tasks/{parentTaskId}/subtasks); an
 *  individually-assigned one never does — it behaves like a subtask itself, with its
 *  progress updated directly via comments rather than rolled up from anything. A
 *  Department-assigned task also gets broken down via CreateSubtaskRequest — its head
 *  Director turns it into a depth-1 team-or-individual "implementation task", one level
 *  deeper, the same endpoint a Team Leader uses to create an ordinary subtask.
 *  source/sourceLabel are optional and open to anyone; severity is optional but
 *  Executive/Super-Admin-only — the service rejects a non-Executive creator's attempt to
 *  set it. */
public record CreateTaskRequest(
    @NotBlank String title,
    String description,
    @NotNull Long createdById,
    Long assignedTeamId,
    Long assignedPersonId,
    Long assignedDepartmentId,
    @NotNull LocalDate dateAssigned,
    @NotNull LocalDate deadline,
    TaskSource source,
    String sourceLabel,
    TaskSeverity severity,
    @NotBlank String openingNote
) {}
