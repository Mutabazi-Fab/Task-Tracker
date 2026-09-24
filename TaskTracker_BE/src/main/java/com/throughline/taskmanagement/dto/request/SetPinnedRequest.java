package com.throughline.taskmanagement.dto.request;

import jakarta.validation.constraints.NotNull;

/** Body for PUT /tasks/{id}/pin. */
public record SetPinnedRequest(
    boolean pinned,
    @NotNull Long changedById
) {}
