package com.throughline.taskmanagement.dto.response;

import com.throughline.taskmanagement.enums.AssigneeType;
import com.throughline.taskmanagement.enums.TaskActivityAction;

import java.time.LocalDateTime;

public record TaskActivityResponse(
    Long id,
    TaskActivityAction action,
    String taskCode,
    String title,
    String parentTaskCode,
    AssigneeType assigneeType,
    String assigneeSummary,
    String performedByName,
    LocalDateTime timestamp
) {}
