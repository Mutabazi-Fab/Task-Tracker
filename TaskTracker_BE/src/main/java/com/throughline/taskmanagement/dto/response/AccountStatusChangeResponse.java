package com.throughline.taskmanagement.dto.response;

import java.time.LocalDateTime;

public record AccountStatusChangeResponse(
    Long id,
    Long personId,
    String personName,
    boolean active,
    String changedByName,
    String reason,
    LocalDateTime timestamp
) {}
