package com.throughline.taskmanagement.dto.response;

import java.time.LocalDateTime;

public record GuidanceNoteResponse(
    Long id,
    String title,
    String body,
    String createdByName,
    LocalDateTime createdAt,
    String updatedByName, // null if never edited since creation
    LocalDateTime updatedAt
) {}
