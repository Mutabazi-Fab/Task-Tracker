package com.throughline.taskmanagement.dto.response;

import com.throughline.taskmanagement.enums.TaskSource;

public record TaskSourceEntryResponse(
    Long id,
    TaskSource source,
    String label
) {}
