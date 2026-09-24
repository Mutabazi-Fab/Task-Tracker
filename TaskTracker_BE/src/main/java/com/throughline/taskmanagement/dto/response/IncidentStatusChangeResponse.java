package com.throughline.taskmanagement.dto.response;

import com.throughline.taskmanagement.enums.IncidentStatus;

import java.time.LocalDateTime;

public record IncidentStatusChangeResponse(
    Long id,
    IncidentStatus fromStatus, // null for the first row
    IncidentStatus toStatus,
    String changedByName,
    String note,
    LocalDateTime changedAt
) {}
