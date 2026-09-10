package com.throughline.taskmanagement.dto.response;

import com.throughline.taskmanagement.enums.CommentType;

import java.time.LocalDateTime;

public record CommentResponse(
    Long id,
    int sequenceNumber,
    String authorName,
    int percentageAtComment,
    String body,
    CommentType type,
    // Null for a PROGRESS comment, or a top-level DISCUSSION one. Set only for a
    // DISCUSSION reply — the id of the top-level comment it replies to.
    Long parentCommentId,
    LocalDateTime createdAt
) {}
