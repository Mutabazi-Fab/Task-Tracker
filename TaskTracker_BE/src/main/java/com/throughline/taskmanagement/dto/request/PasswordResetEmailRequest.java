package com.throughline.taskmanagement.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Body for both POST /auth/password-reset-requests/check and POST
 *  /auth/password-reset-requests — same shape, just an email either way. @Email failing
 *  validation is what backs the frontend's "Please enter a valid email address" message;
 *  everything past that (exists or not, already pending or not) is a real response body,
 *  not a validation error, per this app's deliberate choice to tell an internal user
 *  whether their account exists rather than stay silent about it. */
public record PasswordResetEmailRequest(
    @Email @NotBlank String email
) {}
