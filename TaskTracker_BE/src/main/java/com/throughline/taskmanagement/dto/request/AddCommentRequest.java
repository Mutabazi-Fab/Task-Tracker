package com.throughline.taskmanagement.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddCommentRequest(
    @NotNull Long authorId,
    @Min(0) @Max(100) int percentageAtComment,
    @NotBlank String body
) {}
