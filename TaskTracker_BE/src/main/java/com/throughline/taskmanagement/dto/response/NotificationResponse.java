package com.throughline.taskmanagement.dto.response;

import com.throughline.taskmanagement.enums.NotificationType;

import java.time.LocalDateTime;

public record NotificationResponse(
    Long id,
    NotificationType type,
    String message,
    Long relatedEntityId,
    boolean isRead,
    LocalDateTime createdAt
) {}
