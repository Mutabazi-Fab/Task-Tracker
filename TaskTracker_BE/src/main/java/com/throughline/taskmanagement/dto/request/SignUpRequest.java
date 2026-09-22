package com.throughline.taskmanagement.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Completes an account a Super Admin already created (no password set yet): the code
 *  sent to their email plus the password they're choosing for themselves. Not public
 *  self-registration — the account has to already exist, pending this step. */
public record SignUpRequest(
    @Email @NotBlank String email,
    @NotBlank String otp,
    @NotBlank @Size(min = 8, message = "Password must be at least 8 characters") String newPassword
) {}
