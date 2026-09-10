package com.throughline.taskmanagement.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** percentageAtComment is genuinely optional now — null means a plain narrative note that
 *  changes nothing about actual progress (never trusted for a TEAM/DEPARTMENT-rolled-up
 *  task either way; see TaskServiceImpl.addProgressComment). @Min/@Max are skipped by Bean
 *  Validation when the value is null, so a narrative-only comment is never rejected for
 *  "missing" a percentage. */
public record AddCommentRequest(
    @NotNull Long authorId,
    @Min(0) @Max(100) Integer percentageAtComment,
    @NotBlank String body
) {}
