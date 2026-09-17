package com.throughline.taskmanagement.dto.response;

/** Backs GET /tasks/{id}/documents/{documentId}/download — the actual bytes, kept separate
 *  from DocumentResponse so a plain task-detail load never has to carry file content. */
public record DocumentDownload(
    String fileName,
    String contentType,
    byte[] content
) {}
