package com.throughline.taskmanagement.dto.request;

import com.throughline.taskmanagement.enums.AssigneeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateTaskRequest(
    @NotBlank String title,
    String description,
    @NotNull Long assignedById,
    @NotNull AssigneeType assigneeType,
    Long assignedPersonId,
    Long assignedTeamId,
    @NotNull LocalDate dateAssigned,
    @NotBlank String openingNote
) {}
