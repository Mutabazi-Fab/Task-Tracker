package com.throughline.taskmanagement.dto.request;

import com.throughline.taskmanagement.enums.IncidentStatus;
import jakarta.validation.constraints.NotNull;

/** Moves an incident to a new status and records it in IncidentStatusChange. */
public record ChangeIncidentStatusRequest(
    @NotNull IncidentStatus newStatus,
    String note,
    @NotNull Long changedById
) {}
