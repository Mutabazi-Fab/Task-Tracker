package com.throughline.taskmanagement.dto.response;

public record PersonResponse(
    Long id,
    String fullName,
    String email,
    String role,
    String rank,
    String teamName,
    Long teamId
) {}
