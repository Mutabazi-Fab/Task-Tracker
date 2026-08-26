package com.throughline.taskmanagement.dto.response;

import com.throughline.taskmanagement.enums.AssigneeType;
import com.throughline.taskmanagement.enums.TaskStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record TaskDetailResponse(
    Long id,
    String taskCode,
    String title,
    String description,
    String assigneeName,
    Long assigneeId,
    AssigneeType assigneeType,
    TaskStatus status,
    int progressPercentage,
    LocalDate dateAssigned,
    String assignedByName,
    Long assignedById,
    List<CommentResponse> comments,
    List<ReassignmentResponse> reassignments,
    List<TaskTimelineResponse> progressTimeline,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
