package com.throughline.taskmanagement.dto.response;

import com.throughline.taskmanagement.enums.ExtensionRequestStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** One row of a task's deadline-extension history — a request, and however it was (or
 *  wasn't yet) decided. decidedByName/decisionNote/decidedAt are all null while status is
 *  PENDING. A direct extension (see TaskServiceImpl.extendDeadlineDirectly) shows up here
 *  exactly like a request that was immediately self-approved. */
public record DeadlineExtensionResponse(
    Long id,
    LocalDate currentDeadline,
    LocalDate requestedDeadline,
    String justification,
    String requestedByName,
    ExtensionRequestStatus status,
    String decidedByName,
    String decisionNote,
    LocalDateTime requestedAt,
    LocalDateTime decidedAt
) {}
