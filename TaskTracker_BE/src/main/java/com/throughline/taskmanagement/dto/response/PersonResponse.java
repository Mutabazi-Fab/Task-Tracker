package com.throughline.taskmanagement.dto.response;

public record PersonResponse(
    Long id,
    String fullName,
    String email,
    String role,
    String teamName,
    Long teamId
) {}
