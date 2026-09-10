package com.throughline.taskmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** Body for POST /tasks/{id}/deadline-extensions. requestedById must be the task's own
 *  accountable person (its Team Leader, its individual assignee, or — for a Department
 *  task — that Department's head Director), or a Director-or-above override (Executive-
 *  or-above for a Department task) — enforced in the service. requestedDeadline must be
 *  after the task's current deadline. */
public record RequestDeadlineExtensionRequest(
    @NotNull LocalDate requestedDeadline,
    @NotBlank String justification,
    @NotNull Long requestedById
) {}
