package com.throughline.taskmanagement.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** One deadline-extension request still awaiting a decision from the viewer specifically —
 *  the "Requests" inbox on the dashboard, not a single task's own history (see
 *  DeadlineExtensionResponse for that). Unlike DeadlineExtensionResponse, this carries the
 *  task's own identity (id/code/title), since a viewer here is looking across every task
 *  they're the decider for, not browsing one task they're already on.
 *
 *  canApprove is specific to the viewer this response was built for (see
 *  TaskServiceImpl.getPendingExtensionRequests/canApproveDeadline) — every viewer who can
 *  see a row at all can reject it, but on a CEO-mandated task chain only an Executive/Super
 *  Admin can approve it, even though the request still lands in a Director's own inbox too
 *  (they're who it was first sent to, and who can still reject it). false tells the
 *  frontend to hide/disable the Approve action for that viewer rather than show a button
 *  that would just fail.
 *
 *  forwardedToApprover is not viewer-specific — an objective fact about the request itself
 *  (see TaskServiceImpl.forwardExtensionRequestToApprover): false means it hasn't been sent
 *  to the CEO/Super Admin's own inbox yet (only the Director it landed on sees it so far);
 *  true means it has. Effectively always true for an ordinary Director-originated task,
 *  where there's nobody to forward it to in the first place — canApprove is already true
 *  for the one person who sees it there. */
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
