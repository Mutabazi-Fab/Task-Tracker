package com.throughline.taskmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** changedById must be a Super Admin — enforced in the service, not just trusted here.
 *  reason is mandatory, same rule as every other admin-triggered change to someone's
 *  account. Clears totpSecret/totpEnabledAt and every recovery code for the person that
 *  lost/replaced their phone — their next login re-enters TOTP enrollment from scratch. */
public record ResetTotpRequest(
    @NotNull Long changedById,
    @NotBlank String reason
) {}
