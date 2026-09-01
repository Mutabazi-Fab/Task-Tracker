package com.throughline.taskmanagement.dto.request;

import com.throughline.taskmanagement.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * No teamId — a person can belong to multiple teams now, so team membership is managed
 * exclusively through TeamService (add/removeMember), not at person-creation time.
 *
 * createdById/role are only meaningful for POST /people (creation) — not @NotNull here
 * (PUT /people/{id}, which reuses this same record for the name/email/jobTitle/rank
 * fields, ignores both and shouldn't have to supply a meaningless value); createPerson
 * itself requires createdById and checks it's a Director or Super Admin. Only a Super
 * Admin may set role to anything other than Member (null defaults to Member). Role
 * changes on an EXISTING person always go through the dedicated PUT /people/{id}/role
 * endpoint, never through here.
 */
public record CreatePersonRequest(
    @NotBlank String fullName,
    @Email @NotBlank String email,
    @NotBlank String jobTitle,
    String rank,
    Long createdById,
    Role role
) {}
