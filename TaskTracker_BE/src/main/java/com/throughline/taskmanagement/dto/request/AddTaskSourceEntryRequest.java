package com.throughline.taskmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Executive-or-above — enforced in TaskSourceEntryServiceImpl, not just trusted here. */
public record AddTaskSourceEntryRequest(
    @NotBlank String source,
    @NotBlank String label,
    @NotNull Long addedById
) {}
