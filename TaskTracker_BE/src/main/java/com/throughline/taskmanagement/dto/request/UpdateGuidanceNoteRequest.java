package com.throughline.taskmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Edits an existing guidance note. Director/Executive/Super Admin only. changedById is
 *  overwritten server-side with the caller's own JWT-resolved identity. */
public record UpdateGuidanceNoteRequest(
    @NotBlank String title,
    @NotBlank String body,
    @NotNull Long changedById
) {}
