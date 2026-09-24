package com.throughline.taskmanagement.dto.request;

import com.throughline.taskmanagement.enums.DataBreachStatus;

import com.throughline.taskmanagement.enums.IncidentCategory;
import com.throughline.taskmanagement.enums.IncidentReviewStatus;
import com.throughline.taskmanagement.enums.RegulatorNotifiableStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/** Edits an incident's investigation/closure fields over its lifecycle — everything except
 *  status (see ChangeIncidentStatusRequest, which also carries its own audit trail) and the
 *  server-generated incidentCode/reportedBy. Director/Executive/Super Admin only, same as
 *  creating one. changedById is overwritten by the controller with the caller's own
 *  JWT-resolved identity. actualClosureDate is set here in preparation for closing, but the
 *  transition to CLOSED itself still goes through ChangeIncidentStatusRequest, which is what
 *  enforces IncidentServiceImpl.requireClosureReadiness. */
public record UpdateIncidentRequest(
    LocalDate dateOccurred,
    LocalTime timeOccurred,
    LocalDate dateDiscovered,
    LocalDate dateReported,
    String businessUnit,
    String locationChannel,
    IncidentCategory category,
    String eventType,
    @NotBlank String title,
    String description,
    Integer likelihood,
    Integer impact,
    Integer customersAffected,
    Integer serviceDowntimeMinutes,
    BigDecimal grossLoss,
    BigDecimal recovery,
    DataBreachStatus dataBreach,
    RegulatorNotifiableStatus regulatorNotifiable,
    LocalDate bnrNotificationDate,
    String immediateContainment,
    String rootCause,
    String correctiveAction,
    Long actionOwnerId,
    LocalDate targetClosureDate,
    LocalDate actualClosureDate,
    String evidenceReference,
    String incidentOwner,
    IncidentReviewStatus riskReview,
    IncidentReviewStatus complianceReview,
    String lessonsLearned,
    @NotNull Long changedById
) {}
