package com.throughline.taskmanagement.dto.response;

import com.throughline.taskmanagement.enums.ActionSlaStatus;
import com.throughline.taskmanagement.enums.DataBreachStatus;

import com.throughline.taskmanagement.enums.IncidentCategory;
import com.throughline.taskmanagement.enums.IncidentReviewStatus;
import com.throughline.taskmanagement.enums.IncidentSeverity;
import com.throughline.taskmanagement.enums.IncidentStatus;
import com.throughline.taskmanagement.enums.RegulatorNotifiableStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/** Full detail view — everything on the incident plus its status history. daysOpen/
 *  actionSla/wasClosedLate/closureReady are all computed fresh at read time (see
 *  IncidentMapper), never persisted, since they depend on TODAY() or on the current values
 *  of other fields. */
public record IncidentDetailResponse(
    Long id,
    String incidentCode,
    LocalDate dateOccurred,
    LocalTime timeOccurred,
    LocalDate dateDiscovered,
    LocalDate dateReported,
    String businessUnit,
    String locationChannel,
    IncidentCategory category,
    String eventType,
    String title,
    String description,
    Integer likelihood,
    Integer impact,
    Integer inherentScore, // null = not yet scored
    IncidentSeverity severity, // null = not yet scored
    Integer customersAffected,
    Integer serviceDowntimeMinutes,
    BigDecimal grossLoss,
    BigDecimal recovery,
    BigDecimal netLoss,
    DataBreachStatus dataBreach,
    RegulatorNotifiableStatus regulatorNotifiable,
    LocalDate bnrNotificationDate,
    IncidentStatus status,
    String immediateContainment,
    String rootCause,
    String correctiveAction,
    Long actionOwnerId,
    String actionOwnerName,
    LocalDate targetClosureDate,
    LocalDate actualClosureDate,
    Long daysOpen,
    ActionSlaStatus actionSla,
    // True only when this incident closed AFTER its own target date — the source Excel's
    // Action SLA formula never flagged this; see ActionSlaStatus.CLOSED_LATE.
    boolean wasClosedLate,
    String evidenceReference,
    Long reportedById,
    String reportedByName,
    String incidentOwner,
    IncidentReviewStatus riskReview,
    IncidentReviewStatus complianceReview,
    String lessonsLearned,
    // Whether ChangeIncidentStatusRequest(CLOSED) would currently succeed — lets the UI
    // show/disable the Close action with an explanation instead of a failed round trip.
    boolean closureReady,
    List<String> closureBlockers,
    List<IncidentStatusChangeResponse> statusHistory,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
