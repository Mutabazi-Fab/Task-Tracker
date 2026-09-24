package com.throughline.taskmanagement.mapper;

import com.throughline.taskmanagement.dto.response.IncidentDetailResponse;
import com.throughline.taskmanagement.dto.response.IncidentListResponse;
import com.throughline.taskmanagement.dto.response.IncidentStatusChangeResponse;
import com.throughline.taskmanagement.enums.ActionSlaStatus;
import com.throughline.taskmanagement.enums.IncidentReviewStatus;
import com.throughline.taskmanagement.enums.IncidentSeverity;
import com.throughline.taskmanagement.enums.IncidentStatus;
import com.throughline.taskmanagement.model.Incident;
import com.throughline.taskmanagement.model.IncidentStatusChange;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Everything TODAY()-dependent (daysOpen, actionSla, wasClosedLate, closureReady) is
 * computed here at read time, never persisted — mirrors the source Excel's own
 * TODAY()-based formulas, but fixes the two gaps it had: a blank Likelihood/Impact showed a
 * literal FALSE instead of a clean "not scored" state, and a late close never got flagged as
 * having breached its own SLA (see ActionSlaStatus.CLOSED_LATE).
 */
@Component
public class IncidentMapper {

    private static final Set<IncidentSeverity> HIGH_TIER = Set.of(IncidentSeverity.HIGH, IncidentSeverity.CRITICAL);

    public IncidentListResponse toListResponse(Incident incident) {
        long daysOpen = computeDaysOpen(incident);
        return new IncidentListResponse(
                incident.getId(),
                incident.getIncidentCode(),
                incident.getTitle(),
                displayBusinessUnit(incident.getBusinessUnit()),
                incident.getCategory(),
                incident.getSeverity(),
                incident.getStatus(),
                incident.getDateOccurred(),
                incident.getTargetClosureDate(),
                daysOpen,
                computeActionSla(incident),
                incident.getNetLoss(),
                incident.getActionOwner() != null ? incident.getActionOwner().getFullName() : null,
                incident.getReportedBy().getFullName()
        );
    }

    public IncidentDetailResponse toDetailResponse(Incident incident) {
        List<String> blockers = closureBlockers(incident);
        List<IncidentStatusChangeResponse> history = new ArrayList<>();
        for (IncidentStatusChange change : incident.getStatusChanges()) {
            history.add(toStatusChangeResponse(change));
        }

        return new IncidentDetailResponse(
                incident.getId(),
                incident.getIncidentCode(),
                incident.getDateOccurred(),
                incident.getTimeOccurred(),
                incident.getDateDiscovered(),
                incident.getDateReported(),
                displayBusinessUnit(incident.getBusinessUnit()),
                incident.getLocationChannel(),
                incident.getCategory(),
                incident.getEventType(),
                incident.getTitle(),
                incident.getDescription(),
                incident.getLikelihood(),
                incident.getImpact(),
                incident.getInherentScore(),
                incident.getSeverity(),
                incident.getCustomersAffected(),
                incident.getServiceDowntimeMinutes(),
                incident.getGrossLoss(),
                incident.getRecovery(),
                incident.getNetLoss(),
                incident.getDataBreach(),
                incident.getRegulatorNotifiable(),
                incident.getBnrNotificationDate(),
                incident.getStatus(),
                incident.getImmediateContainment(),
                incident.getRootCause(),
                incident.getCorrectiveAction(),
                incident.getActionOwner() != null ? incident.getActionOwner().getId() : null,
                incident.getActionOwner() != null ? incident.getActionOwner().getFullName() : null,
                incident.getTargetClosureDate(),
                incident.getActualClosureDate(),
                computeDaysOpen(incident),
                computeActionSla(incident),
                wasClosedLate(incident),
                incident.getEvidenceReference(),
                incident.getReportedBy().getId(),
                incident.getReportedBy().getFullName(),
                incident.getIncidentOwner(),
                incident.getRiskReview(),
                incident.getComplianceReview(),
                incident.getLessonsLearned(),
                blockers.isEmpty(),
                blockers,
                history,
                incident.getCreatedAt(),
                incident.getUpdatedAt()
        );
    }

    public IncidentStatusChangeResponse toStatusChangeResponse(IncidentStatusChange change) {
        return new IncidentStatusChangeResponse(
                change.getId(),
                change.getFromStatus(),
                change.getToStatus(),
                change.getChangedBy().getFullName(),
                change.getNote(),
                change.getChangedAt()
        );
    }

    /** New incidents store a department's name as typed ("Information Technology"). Rows
     *  recorded before Business Unit became department-driven hold the old enum constant
     *  ("INFORMATION_TECHNOLOGY") — left untouched in the database and just re-spelled here
     *  as "Information Technology" when read, so nothing has to be rewritten. */
    public String displayBusinessUnit(String stored) {
        if (stored == null || !stored.matches("[A-Z_]+")) {
            return stored;
        }
        StringBuilder words = new StringBuilder();
        for (String word : stored.split("_")) {
            if (word.isEmpty()) continue;
            if (words.length() > 0) words.append(' ');
            words.append(word.charAt(0)).append(word.substring(1).toLowerCase());
        }
        return words.toString();
    }

    /** = likelihood * impact, or null if either is missing — matches the source formula
     *  `IF(OR(L="",M=""),"",L*M)` exactly, just without its FALSE-on-blank edge case since
     *  null simply renders as "not yet scored" in the UI. */
    public Integer computeInherentScore(Integer likelihood, Integer impact) {
        if (likelihood == null || impact == null) return null;
        return likelihood * impact;
    }

    /** Bands an inherent score into a severity — null in, null out (see computeInherentScore).
     *  1-5 Low, 6-10 Moderate, 11-15 High, 16-25 Critical — same bands as the Excel's own
     *  Lists & Guidance sheet, with "Moderate" instead of the formula's inconsistent "Medium". */
    public IncidentSeverity computeSeverity(Integer inherentScore) {
        if (inherentScore == null) return null;
        if (inherentScore <= 5) return IncidentSeverity.LOW;
        if (inherentScore <= 10) return IncidentSeverity.MODERATE;
        if (inherentScore <= 15) return IncidentSeverity.HIGH;
        return IncidentSeverity.CRITICAL;
    }

    private long computeDaysOpen(Incident incident) {
        LocalDate end = incident.getActualClosureDate() != null ? incident.getActualClosureDate() : LocalDate.now();
        return ChronoUnit.DAYS.between(incident.getDateDiscovered(), end);
    }

    private ActionSlaStatus computeActionSla(Incident incident) {
        if (incident.getStatus() == IncidentStatus.CLOSED) {
            return wasClosedLate(incident) ? ActionSlaStatus.CLOSED_LATE : ActionSlaStatus.CLOSED;
        }
        LocalDate target = incident.getTargetClosureDate();
        if (target == null) return ActionSlaStatus.NO_DUE_DATE;
        LocalDate today = LocalDate.now();
        if (target.isBefore(today)) return ActionSlaStatus.OVERDUE;
        if (!target.isAfter(today.plusDays(7))) return ActionSlaStatus.DUE_SOON;
        return ActionSlaStatus.ON_TRACK;
    }

    /** True only when this incident closed strictly after its own target date — the gap the
     *  source Excel's Action SLA formula never covered (it just showed "Closed" either way). */
    private boolean wasClosedLate(Incident incident) {
        return incident.getStatus() == IncidentStatus.CLOSED
                && incident.getTargetClosureDate() != null
                && incident.getActualClosureDate() != null
                && incident.getActualClosureDate().isAfter(incident.getTargetClosureDate());
    }

    /** What's still missing before ChangeIncidentStatusRequest(CLOSED) would succeed — see
     *  IncidentServiceImpl.requireClosureReadiness, which enforces the same rules. Kept here
     *  too so the read side (closureReady/closureBlockers) never drifts out of sync with the
     *  write side. */
    private List<String> closureBlockers(Incident incident) {
        List<String> blockers = new ArrayList<>();
        if (incident.getStatus() == IncidentStatus.CLOSED) {
            return blockers;
        }
        if (blank(incident.getRootCause())) {
            blockers.add("Root cause is required before closing.");
        }
        if (blank(incident.getCorrectiveAction())) {
            blockers.add("Corrective / preventive action is required before closing.");
        }
        boolean needsReview = HIGH_TIER.contains(incident.getSeverity())
                || incident.getRegulatorNotifiable() == com.throughline.taskmanagement.enums.RegulatorNotifiableStatus.YES
                || incident.getRegulatorNotifiable() == com.throughline.taskmanagement.enums.RegulatorNotifiableStatus.ASSESS;
        if (needsReview) {
            if (incident.getRiskReview() != IncidentReviewStatus.COMPLETED) {
                blockers.add("Risk review must be completed before closing a Critical/High or regulator-notifiable incident.");
            }
            if (incident.getComplianceReview() != IncidentReviewStatus.COMPLETED) {
                blockers.add("Compliance review must be completed before closing a Critical/High or regulator-notifiable incident.");
            }
        }
        return blockers;
    }

    private boolean blank(String s) {
        return s == null || s.isBlank();
    }
}
