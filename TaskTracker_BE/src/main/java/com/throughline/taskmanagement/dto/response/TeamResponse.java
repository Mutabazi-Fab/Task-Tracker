package com.throughline.taskmanagement.dto.response;

public record TeamResponse(
    Long id,
    String name,
    String leaderName,
    Long leaderId,
    int memberCount
) {}
