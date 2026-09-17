package com.throughline.taskmanagement.dto.response;

import java.time.LocalDateTime;

/** One supporting document attached to a task — metadata only, never the bytes (those come
 *  back only from the dedicated GET /tasks/{id}/documents/{documentId}/download endpoint, so
 *  a task's own detail response never balloons with file content on every load). */
public record DocumentResponse(
    Long id,
    String fileName,
    String contentType,
    long fileSize,
    String uploadedByName,
    Long uploadedById,
    LocalDateTime uploadedAt
) {}
