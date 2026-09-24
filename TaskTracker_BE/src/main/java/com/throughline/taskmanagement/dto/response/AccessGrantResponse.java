package com.throughline.taskmanagement.dto.response;

import com.throughline.taskmanagement.enums.AccessResourceType;

import java.time.LocalDateTime;

public record AccessGrantResponse(
    Long id,
    Long granteeId,
    String granteeName,
    String granteeJobTitle,
    AccessResourceType resourceType,
    Long resourceId,
    String resourceCode,
    String resourceTitle,
    String grantedByName,
    String reason,
    LocalDateTime grantedAt
) {}
