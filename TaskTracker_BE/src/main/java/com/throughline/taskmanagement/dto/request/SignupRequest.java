package com.throughline.taskmanagement.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * No `role` field here on purpose — self-service signup always creates a
 * MEMBER (see AuthServiceImpl). Director accounts are provisioned out of
 * band for now; there is no self-escalation path.
 */
public record SignupRequest(
    @NotBlank String fullName,
    @Email @NotBlank String email,
    @NotBlank @Size(min = 8, message = "Password must be at least 8 characters") String password,
    @NotBlank String jobTitle,
    String rank
) {}
