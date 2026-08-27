package com.throughline.taskmanagement.dto.response;

import com.throughline.taskmanagement.enums.Role;

import java.util.List;

public record PersonResponse(
    Long id,
    String fullName,
    String email,
    String jobTitle,
    String rank,
    Role role,
    List<PersonTeamMembershipResponse> teams
) {}
