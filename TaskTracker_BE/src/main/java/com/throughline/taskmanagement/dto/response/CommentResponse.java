package com.throughline.taskmanagement.dto.response;

import java.time.LocalDateTime;

public record CommentResponse(
    Long id,
    int sequenceNumber,
    String authorName,
    int percentageAtComment,
    String body,
    LocalDateTime createdAt
) {}
