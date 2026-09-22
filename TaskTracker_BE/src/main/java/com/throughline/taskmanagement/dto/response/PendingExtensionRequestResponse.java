package com.throughline.taskmanagement.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** One deadline-extension request still awaiting a decision from the viewer specifically —
 *  the "Requests" inbox, not a single task's own history (see DeadlineExtensionResponse).
 *  Carries the task's own identity (id/code/title) since the viewer is looking across
 *  every task they're the decider for.
 *
 *  canApprove is viewer-specific (see TaskServiceImpl.getPendingExtensionRequests/
 *  canApproveDeadline): everyone who sees a row can reject it, but on a CEO-mandated chain
 *  only an Executive/Super Admin can approve it, even though it still lands in a
 *  Director's inbox too. false tells the frontend to hide/disable Approve for that viewer.
 *
 *  forwardedToApprover is an objective fact about the request itself (see
 *  TaskServiceImpl.forwardExtensionRequestToApprover) — false means it hasn't reached the
 *  CEO/Super Admin's inbox yet. Effectively always true for an ordinary Director-originated
 *  task, which has nobody to forward to in the first place. */
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
