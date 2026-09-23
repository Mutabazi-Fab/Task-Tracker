package com.throughline.taskmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;

/** Completes a login for an already-enrolled account: the pendingAuthToken login handed
 *  back, plus either a live 6-digit TOTP code or an unused recovery code. */
public record TotpVerifyRequest(
    @NotBlank String pendingAuthToken,
    @NotBlank String code
) {}
