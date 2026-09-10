package com.throughline.taskmanagement.dto.request;

import jakarta.validation.constraints.NotNull;

public record ChangeDepartmentHeadRequest(
    @NotNull Long newHeadDirectorId,
    @NotNull Long changedById
) {}
