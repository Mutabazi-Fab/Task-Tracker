package com.throughline.taskmanagement.dto.request;

import jakarta.validation.constraints.NotNull;

/** Body for PUT /tasks/{id}/pin. Director-or-above only, enforced in the service. A
 *  manual, independently-editable toggle — not derived from severity, so this can freely
 *  pin or unpin any task regardless of its severity classification. */
public record SetPinnedRequest(
    boolean pinned,
    @NotNull Long changedById
) {}
