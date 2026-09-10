package com.throughline.taskmanagement.dto.response;

import java.time.LocalDateTime;

public record DepartmentResponse(
    Long id,
    String name,
    String headDirectorName,
    Long headDirectorId,
    int teamCount,
    String createdByName,
    LocalDateTime createdAt
) {}
