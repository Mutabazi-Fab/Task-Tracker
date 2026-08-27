package com.throughline.taskmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** Creates a TOP-LEVEL task only — Director-only, always assigned to a team.
 *  Use CreateSubtaskRequest (POST /tasks/{parentTaskId}/subtasks) for subtasks. */
public record CreateTaskRequest(
    @NotBlank String title,
    String description,
    @NotNull Long createdById,
    @NotNull Long assignedTeamId,
    @NotNull LocalDate dateAssigned,
    @NotBlank String openingNote
) {}
