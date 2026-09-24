package com.throughline.taskmanagement.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** One deadline-extension request still awaiting a decision from the viewer specifically — the
 *  "Requests" inbox, not a single task's own history (see DeadlineExtensionResponse). */
public record PendingExtensionRequestResponse(
    Long id,
    Long taskId,
    String taskCode,
    String taskTitle,
    LocalDate currentDeadline,
    LocalDate requestedDeadline,
    String justification,
    String requestedByName,
    LocalDateTime requestedAt,
    boolean canApprove,
    boolean forwardedToApprover
) {}
