package com.throughline.taskmanagement.dto.response;

import java.time.LocalDateTime;

public record TaskTimelineResponse(
    int percentage,
    LocalDateTime date,
    Long commentId
) {}
