package com.throughline.taskmanagement.dto.request;

import com.throughline.taskmanagement.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * No teamId — a person can belong to multiple teams, so membership is managed exclusively
 * through TeamService (add/removeMember), not at person-creation time.
 *
 * createdById/role/departmentId/password are only meaningful for POST /people (creation),
 * not @NotNull here — PUT /people/{id} reuses this same record for the name/email/jobTitle/
 * rank fields and ignores all four. createPerson itself requires and validates each one
 * (Super-Admin-only creator, a department, an 8+ char password). Role changes on an
 * EXISTING person always go through PUT /people/{id}/role, never through here.
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
