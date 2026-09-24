package com.throughline.taskmanagement.dto.response;

import com.throughline.taskmanagement.enums.ActionSlaStatus;

import com.throughline.taskmanagement.enums.IncidentCategory;
import com.throughline.taskmanagement.enums.IncidentSeverity;
import com.throughline.taskmanagement.enums.IncidentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Row shape for the incident list/search — everything a table view needs without pulling
 *  the full status history per row. daysOpen/actionSla are computed fresh at read time (see
 *  IncidentMapper), never persisted, since they depend on TODAY(). */
public record IncidentListResponse(
    Long id,
    String incidentCode,
    String title,
    String businessUnit,
    IncidentCategory category,
    IncidentSeverity severity, // null = not yet scored
    IncidentStatus status,
    LocalDate dateOccurred,
    LocalDate targetClosureDate,
    Long daysOpen,
    ActionSlaStatus actionSla,
    BigDecimal netLoss,
    String actionOwnerName, // null if not yet assigned
    String reportedByName
) {}
