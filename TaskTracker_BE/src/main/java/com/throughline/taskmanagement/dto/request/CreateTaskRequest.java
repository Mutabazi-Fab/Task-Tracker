package com.throughline.taskmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** Creates a TOP-LEVEL task only — Director-only. Assigned to either a team OR a single
 *  individual directly: exactly one of assignedTeamId/assignedPersonId must be set (the
 *  service rejects both or neither). A team-assigned top-level task later gets broken into
 *  subtasks (see CreateSubtaskRequest, POST /tasks/{parentTaskId}/subtasks); an
 *  individually-assigned one never does — it behaves like a subtask itself, with its
 *  progress updated directly via comments rather than rolled up from anything. */
public record CreateTaskRequest(
    @NotBlank String title,
    String description,
    @NotNull Long createdById,
    Long assignedTeamId,
    Long assignedPersonId,
    @NotNull LocalDate dateAssigned,
    @NotBlank String openingNote
) {}
