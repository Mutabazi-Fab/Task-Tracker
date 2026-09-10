package com.throughline.taskmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * No newAssigneeType — which one applies is implied by the task's own shape, not a client
 * choice. Send newTeamId/newPersonId for anything TEAM/INDIVIDUAL-assigned (a top-level
 * task, a depth-1 Department implementation task, or an ordinary leaf subtask — the
 * service enforces the right scoping for each), or newDepartmentId to move a Department-
 * level task to a different Department (Executive/Super Admin only) — the service rejects
 * the wrong one for the task's actual shape.
 */
public record ReassignTaskRequest(
    Long newTeamId,
    Long newPersonId,
    Long newDepartmentId,
    @NotNull Long reassignedById,
    @NotBlank String reason
) {}
