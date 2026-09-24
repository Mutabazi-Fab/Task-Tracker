package com.throughline.taskmanagement.dto.request;

import com.throughline.taskmanagement.enums.TaskSeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** Creates a TOP-LEVEL (depth 0) task — Director-or-above. */
public record CreateTaskRequest(
    @NotBlank String title,
    String description,
    @NotNull Long createdById,
    Long assignedTeamId,
    Long assignedPersonId,
    Long assignedDepartmentId,
    @NotNull LocalDate dateAssigned,
    @NotNull LocalDate deadline,
    String source,
    String sourceLabel,
    TaskSeverity severity,
    @NotBlank String openingNote
) {}
