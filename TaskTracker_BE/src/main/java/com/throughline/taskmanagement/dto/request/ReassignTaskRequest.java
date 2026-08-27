package com.throughline.taskmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * No newAssigneeType — which one applies is implied by the task's hierarchy level, not a
 * client choice. Send newTeamId for a top-level task (moves it to a different team) or
 * newPersonId for a subtask (moves it to a different member of the SAME team); the service
 * rejects the wrong one for the task's level.
 */
public record ReassignTaskRequest(
    Long newTeamId,
    Long newPersonId,
    @NotNull Long reassignedById,
    @NotBlank String reason
) {}
