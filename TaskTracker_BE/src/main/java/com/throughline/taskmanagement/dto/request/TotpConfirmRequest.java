package com.throughline.taskmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;

/** Completes TOTP enrollment: the pendingAuthToken login handed back, plus the 6-digit
 *  code generated from the QR code just scanned. */
public record TotpConfirmRequest(
    @NotBlank String pendingAuthToken,
    @NotBlank String code
) {}
