package com.throughline.taskmanagement.dto.response;

import com.throughline.taskmanagement.enums.Role;

import java.time.LocalDateTime;
import java.util.List;

public record PersonResponse(
    Long id,
    String fullName,
    String email,
    String jobTitle,
    String rank,
    Role role,
    boolean active,
    boolean totpRequired,
    boolean totpEnabled,
    /** Null unless this person has an open (PENDING) password-reset request awaiting a Super Admin — see
     *  PasswordResetRequest. */
    LocalDateTime pendingPasswordResetRequestedAt,
    List<PersonTeamMembershipResponse> teams,
    String departmentName,
    Long departmentId
) {}
