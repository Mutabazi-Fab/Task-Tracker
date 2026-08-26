package com.throughline.taskmanagement.dto.response;

import java.time.LocalDateTime;

public record ReassignmentResponse(
    Long id,
    String fromName,
    String toName,
    String reassignedByName,
    String reason,
    LocalDateTime reassignedAt
) {}
