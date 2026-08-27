package com.throughline.taskmanagement.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** No teamId — a person can belong to multiple teams now, so team membership is managed
 *  exclusively through TeamService (add/removeMember), not at person-creation time. */
public record CreatePersonRequest(
    @NotBlank String fullName,
    @Email @NotBlank String email,
    @NotBlank String jobTitle,
    String rank
) {}
