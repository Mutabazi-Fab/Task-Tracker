package com.throughline.taskmanagement.dto.request;

import jakarta.validation.constraints.NotNull;

/** Body for PUT /tasks/{id}/deadline-extensions/{extensionId}. decidedById must be the
 *  task's own setter (assignedBy) or a Director-or-above override (Executive-or-above for
 *  a Department task) — enforced in the service, same tier as extending directly.
 *  decisionNote is optional — a plain approve/reject doesn't always need an explanation,
 *  unlike a reassignment's mandatory reason. */
public record DecideDeadlineExtensionRequest(
    @NotNull Boolean approve,
    String decisionNote,
    @NotNull Long decidedById
) {}
