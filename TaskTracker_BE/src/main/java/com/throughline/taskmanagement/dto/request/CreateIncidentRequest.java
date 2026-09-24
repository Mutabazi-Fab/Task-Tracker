package com.throughline.taskmanagement.dto.request;

import com.throughline.taskmanagement.enums.DataBreachStatus;

import com.throughline.taskmanagement.enums.IncidentCategory;
import com.throughline.taskmanagement.enums.RegulatorNotifiableStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/** Reports a new incident — Director/Executive/Super Admin only (Role.isAtLeastDirector),
 *  enforced in IncidentServiceImpl, not just hidden client-side. reportedById is always
 *  overwritten by the controller with the caller's own JWT-resolved identity, same pattern
 *  as CreateTaskRequest.createdById. status always starts OPEN and riskReview/
 *  complianceReview always start PENDING — none of the three are settable here.
 *  likelihood/impact are optional (an incident can be logged before it's scored); when both
 *  are present, inherentScore/severity are computed server-side, never accepted from the
 *  client. */
public record CreateIncidentRequest(
    @NotNull LocalDate dateOccurred,
    LocalTime timeOccurred,
    @NotNull LocalDate dateDiscovered,
    @NotNull LocalDate dateReported,
    @NotBlank String businessUnit,
    String locationChannel,
    @NotNull IncidentCategory category,
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
    String evidenceReference,
    @NotNull Long reportedById,
    String incidentOwner,
    String lessonsLearned
) {}
