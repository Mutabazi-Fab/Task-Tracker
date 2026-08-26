package com.throughline.taskmanagement.dto.response;

public record PersonTaskHistoryResponse(
    Long taskId,
    String taskCode,
    String title,
    String involvementLabel
) {}
