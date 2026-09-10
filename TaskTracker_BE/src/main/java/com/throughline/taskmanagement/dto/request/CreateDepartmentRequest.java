package com.throughline.taskmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Super-Admin-only — enforced in DepartmentServiceImpl, not just trusted here.
 *  headDirectorId must already hold the DIRECTOR role (or above); a department without a
 *  head accountable for it defeats the entire point of this tier. */
public record CreateDepartmentRequest(
    @NotBlank String name,
    @NotNull Long headDirectorId,
    @NotNull Long createdById
) {}
