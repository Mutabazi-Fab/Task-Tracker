package com.throughline.taskmanagement.dto.response;

import com.throughline.taskmanagement.enums.Role;

import java.time.LocalDateTime;

public record RoleChangeResponse(
    Long id,
    Long personId,
    String personName,
    Role oldRole,
    Role newRole,
    String changedByName,
    String reason,
    LocalDateTime timestamp
) {}
