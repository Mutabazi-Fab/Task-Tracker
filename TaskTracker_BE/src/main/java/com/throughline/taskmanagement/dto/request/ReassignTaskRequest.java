package com.throughline.taskmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** No newAssigneeType — which one applies is implied by the task's own shape, not a client choice. */
public record ReassignTaskRequest(
    Long newTeamId,
    Long newPersonId,
    Long newDepartmentId,
    @NotNull Long reassignedById,
    @NotBlank String reason
) {}
