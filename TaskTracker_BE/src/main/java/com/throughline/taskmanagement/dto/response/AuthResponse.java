package com.throughline.taskmanagement.dto.response;

import com.throughline.taskmanagement.enums.Role;

/** token is null when signup succeeds but the email still needs OTP verification — the
 *  frontend should route to "verify your email" rather than treating a null token as an
 *  error. emailVerified tells it which case this is without inspecting the token. */
public record AuthResponse(
    String token,
    Long personId,
    String fullName,
    String email,
    Role role,
    boolean emailVerified
) {}
