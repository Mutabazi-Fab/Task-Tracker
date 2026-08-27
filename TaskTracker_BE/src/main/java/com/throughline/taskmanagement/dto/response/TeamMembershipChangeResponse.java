package com.throughline.taskmanagement.dto.response;

import com.throughline.taskmanagement.enums.TeamMembershipChangeAction;

import java.time.LocalDateTime;

public record TeamMembershipChangeResponse(
    Long id,
    Long teamId,
    String teamName,
    Long personId,
    String personName,
    TeamMembershipChangeAction action,
    String changedByName,
    String reason,
    LocalDateTime timestamp
) {}
