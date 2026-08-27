package com.throughline.taskmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** createdById must be either the parent task's team leader or a Director (the Director
 *  bypassing the Team Leader is explicitly allowed) — enforced in the service.
 *  assignedPersonId must be a member of the parent task's team. */
public record CreateSubtaskRequest(
    @NotBlank String title,
    String description,
    @NotNull Long createdById,
    @NotNull Long assignedPersonId,
    @NotNull LocalDate dateAssigned,
    @NotBlank String openingNote
) {}
