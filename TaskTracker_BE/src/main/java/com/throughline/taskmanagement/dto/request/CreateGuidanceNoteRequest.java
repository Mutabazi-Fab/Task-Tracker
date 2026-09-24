package com.throughline.taskmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Adds a new incident-guidance note — Director/Executive/Super Admin only, same tier as
 *  reporting an incident (Role.isAtLeastDirector). createdById is overwritten server-side
 *  with the caller's own JWT-resolved identity. */
public record CreateGuidanceNoteRequest(
    @NotBlank String title,
    @NotBlank String body,
    @NotNull Long createdById
) {}
