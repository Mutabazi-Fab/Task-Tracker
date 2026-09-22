package com.throughline.taskmanagement.dto.response;

public record TaskSourceEntryResponse(
    Long id,
    String source,
    String label
) {}
