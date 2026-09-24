package com.throughline.taskmanagement.dto.request;

import com.throughline.taskmanagement.enums.IncidentStatus;
import jakarta.validation.constraints.NotNull;

/** Moves an incident to a new status and records it in IncidentStatusChange. Transitioning
 *  to CLOSED is rejected unless IncidentServiceImpl.requireClosureReadiness passes (root
 *  cause + corrective action filled, and for CRITICAL/HIGH severity or a YES/ASSESS
 *  regulator-notifiable incident, both riskReview and complianceReview COMPLETED) —
 *  operationalizing the source Excel's "minimum completion requirements" text as a real
 *  guardrail instead of an unenforced policy note. changedById is overwritten by the
 *  controller with the caller's own JWT-resolved identity. */
public record ChangeIncidentStatusRequest(
    @NotNull IncidentStatus newStatus,
    String note,
    @NotNull Long changedById
) {}
