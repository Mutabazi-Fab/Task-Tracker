package com.throughline.taskmanagement.dto.response;

import com.throughline.taskmanagement.enums.Role;

public record AuthResponse(
    String token,
    Long personId,
    String fullName,
    String email,
    Role role
) {}
