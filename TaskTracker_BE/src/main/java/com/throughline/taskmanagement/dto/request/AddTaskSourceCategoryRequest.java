package com.throughline.taskmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Executive-or-above — enforced in TaskSourceCategoryServiceImpl, not just trusted here. */
public record AddTaskSourceCategoryRequest(
    @NotBlank String name,
    @NotNull Long addedById
) {}
