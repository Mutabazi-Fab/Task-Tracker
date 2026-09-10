package com.throughline.taskmanagement.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** One deadline-extension request still awaiting a decision from the viewer specifically —
 *  the "Requests" inbox on the dashboard, not a single task's own history (see
 *  DeadlineExtensionResponse for that). Unlike DeadlineExtensionResponse, this carries the
 *  task's own identity (id/code/title), since a viewer here is looking across every task
 *  they're the decider for, not browsing one task they're already on. */
public record PendingExtensionRequestResponse(
    Long id,
    Long taskId,
    String taskCode,
    String taskTitle,
    LocalDate currentDeadline,
    LocalDate requestedDeadline,
    String justification,
    String requestedByName,
    LocalDateTime requestedAt
) {}
