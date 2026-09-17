package com.throughline.taskmanagement.dto.request;

import com.throughline.taskmanagement.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * No teamId — a person can belong to multiple teams now, so team membership is managed
 * exclusively through TeamService (add/removeMember), not at person-creation time.
 *
 * createdById/role/departmentId/password are only meaningful for POST /people (creation) —
 * not @NotNull here (PUT /people/{id}, which reuses this same record for the name/email/
 * jobTitle/rank fields, ignores all four and shouldn't have to supply a meaningless value);
 * createPerson itself requires createdById and checks it's a Super Admin (only a Super
 * Admin may create a login-enabled account at all — there is no public self-registration),
 * separately requires departmentId — every person belongs to exactly one department,
 * independent of team membership — and separately requires password (at least 8
 * characters), which createPerson hashes via the same PasswordEncoder login checks against,
 * so the account can log in immediately with the credentials the Super Admin hands them.
 * Role changes on an EXISTING person always go through the dedicated PUT /people/{id}/role
 * endpoint, never through here; department reassignment (once supported) will follow the
 * same pattern.
 */
public record CreatePersonRequest(
    @NotBlank String fullName,
    @Email @NotBlank String email,
    @NotBlank String jobTitle,
    String rank,
    Long createdById,
    Role role,
    Long departmentId,
    String password
) {}
