package com.throughline.taskmanagement.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** Body for PUT /tasks/{id}/deadline — a direct extension, no approval round-trip.
 *  extendedById must be the task's own setter (assignedBy) or a Director-or-above
 *  override (Executive-or-above for a Department task) — same tier as deciding a
 *  request, enforced in the service. Still logged as a self-approved
 *  TaskDeadlineExtensionRequest row, so the audit trail never has a gap just because the
 *  shortcut path was used. reason is optional — defaults to a generic note if omitted. */
public record ExtendDeadlineRequest(
    @NotNull LocalDate newDeadline,
    String reason,
    @NotNull Long extendedById
) {}
