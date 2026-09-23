package com.throughline.taskmanagement.dto.request;

import jakarta.validation.constraints.NotNull;

/** Body for POST /people/{id}/password-reset-request/dismiss. Super-Admin-only.
 *  Deliberately no mandatory reason — dismissing is the lower-consequence action (nothing
 *  on the account actually changes), unlike setPasswordDirectly. */
public record DismissPasswordResetRequestRequest(
    @NotNull Long changedById
) {}
