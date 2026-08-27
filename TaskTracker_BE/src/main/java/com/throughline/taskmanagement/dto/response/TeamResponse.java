package com.throughline.taskmanagement.dto.response;

import java.time.LocalDateTime;

public record TeamResponse(
    Long id,
    String name,
    String createdByName,
    Long createdById,
    String leaderName,
    Long leaderId,
    int memberCount,
    LocalDateTime createdAt
) {}
