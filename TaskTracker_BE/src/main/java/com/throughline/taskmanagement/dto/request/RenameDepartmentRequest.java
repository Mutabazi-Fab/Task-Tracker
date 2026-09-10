package com.throughline.taskmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RenameDepartmentRequest(
    @NotBlank String name,
    @NotNull Long changedById
) {}
