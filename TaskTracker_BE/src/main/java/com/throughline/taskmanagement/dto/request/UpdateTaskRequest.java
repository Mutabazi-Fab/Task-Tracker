package com.throughline.taskmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record UpdateTaskRequest(
    @NotBlank String title,
    String description,
    @NotNull LocalDate dateAssigned
) {}
