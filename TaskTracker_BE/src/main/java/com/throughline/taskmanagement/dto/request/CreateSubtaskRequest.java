package com.throughline.taskmanagement.dto.request;

import com.throughline.taskmanagement.enums.TaskSeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** Two shapes, depending on the parent task's own assigneeType (see TaskServiceImpl.createSubtask):
 *  Parent is TEAM-assigned (an ordinary top-level task, or a depth-1 Department implementation task):
 *  the classic leaf-subtask case. createdById must be either the parent task's team leader or a
 *  Director/Super Admin; assignedPersonId is required and must be a member of the parent task's team;
 *  assignedTeamId must be omitted. */
public record CreateSubtaskRequest(
    @NotBlank String title,
    String description,
    @NotNull Long createdById,
    Long assignedPersonId,
    Long assignedTeamId,
    @NotNull LocalDate dateAssigned,
    @NotNull LocalDate deadline,
    String source,
    String sourceLabel,
    TaskSeverity severity,
    @NotBlank String openingNote
) {}
