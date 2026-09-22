package com.throughline.taskmanagement.dto.request;

import com.throughline.taskmanagement.enums.TaskSource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Executive-or-above — enforced in TaskSourceEntryServiceImpl, not just trusted here. */
public record AddTaskSourceEntryRequest(
    @NotNull TaskSource source,
    @NotBlank String label,
    @NotNull Long addedById
) {}
