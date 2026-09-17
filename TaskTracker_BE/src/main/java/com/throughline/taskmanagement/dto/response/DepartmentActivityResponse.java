package com.throughline.taskmanagement.dto.response;

import java.time.LocalDateTime;

public record DepartmentActivityResponse(
    Long id,
    String departmentName,
    String performedByName,
    LocalDateTime timestamp
) {}
