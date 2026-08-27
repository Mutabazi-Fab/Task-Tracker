package com.throughline.taskmanagement.dto.response;

import java.time.LocalDateTime;

public record TeamMemberResponse(
    Long personId,
    String fullName,
    String jobTitle,
    String rank,
    boolean isLeader,
    LocalDateTime joinedAt
) {}
