package com.throughline.taskmanagement.dto.request;

import com.throughline.taskmanagement.enums.AssigneeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReassignTaskRequest(
    @NotNull AssigneeType newAssigneeType,
    Long newPersonId,
    Long newTeamId,
    @NotNull Long reassignedById,
    @NotBlank String reason
) {}
