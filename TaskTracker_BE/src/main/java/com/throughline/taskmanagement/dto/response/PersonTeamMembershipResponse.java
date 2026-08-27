package com.throughline.taskmanagement.dto.response;

public record PersonTeamMembershipResponse(
    Long teamId,
    String teamName,
    boolean isLeader
) {}
