package com.throughline.taskmanagement.service.impl;

import com.throughline.taskmanagement.dto.request.ChangeIncidentStatusRequest;
import com.throughline.taskmanagement.dto.request.CreateIncidentRequest;
import com.throughline.taskmanagement.dto.request.UpdateIncidentRequest;
import com.throughline.taskmanagement.dto.response.IncidentCountResponse;
import com.throughline.taskmanagement.dto.response.IncidentDashboardResponse;
import com.throughline.taskmanagement.dto.response.IncidentDetailResponse;
import com.throughline.taskmanagement.dto.response.IncidentListResponse;
import com.throughline.taskmanagement.dto.response.IncidentMonthlyCountResponse;

import com.throughline.taskmanagement.enums.IncidentCategory;
import com.throughline.taskmanagement.enums.IncidentSeverity;
import com.throughline.taskmanagement.enums.IncidentStatus;
import com.throughline.taskmanagement.enums.Role;
import com.throughline.taskmanagement.exception.ForbiddenActionException;
import com.throughline.taskmanagement.exception.InvalidAssignmentException;
import com.throughline.taskmanagement.exception.ResourceNotFoundException;
import com.throughline.taskmanagement.mapper.IncidentMapper;
import com.throughline.taskmanagement.model.Incident;
import com.throughline.taskmanagement.model.IncidentStatusChange;
import com.throughline.taskmanagement.model.Person;
import com.throughline.taskmanagement.repository.DepartmentRepository;
import com.throughline.taskmanagement.repository.IncidentRepository;
import com.throughline.taskmanagement.repository.IncidentStatusChangeRepository;
import com.throughline.taskmanagement.repository.PersonRepository;
import com.throughline.taskmanagement.service.IncidentService;
import com.throughline.taskmanagement.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

@Service
@Transactional
@RequiredArgsConstructor
public class IncidentServiceImpl implements IncidentService {

    private static final Set<IncidentSeverity> HIGH_TIER = Set.of(IncidentSeverity.HIGH, IncidentSeverity.CRITICAL);
    private static final Set<IncidentStatus> OPEN_OR_MONITORING = Set.of(
            IncidentStatus.OPEN, IncidentStatus.UNDER_INVESTIGATION, IncidentStatus.MONITORING);

    private final IncidentRepository incidentRepository;
    private final IncidentStatusChangeRepository incidentStatusChangeRepository;
    private final PersonRepository personRepository;
    private final DepartmentRepository departmentRepository;
    private final IncidentMapper incidentMapper;
    private final NotificationService notificationService;

    @Override
    public IncidentDetailResponse createIncident(CreateIncidentRequest request) {
        Person reportedBy = personRepository.findById(request.reportedById())
                .orElseThrow(() -> new ResourceNotFoundException("reportedById not found"));
        if (!Role.isAtLeastDirector(reportedBy.getRole())) {
            throw new ForbiddenActionException("Only a Director, Executive, or Super Admin can report an incident.");
        }
        requireTargetClosureDateNotInPast(request.targetClosureDate());

        Incident incident = new Incident();
        incident.setIncidentCode(generateIncidentCode());
        incident.setDateOccurred(request.dateOccurred());
        incident.setTimeOccurred(request.timeOccurred());
        incident.setDateDiscovered(request.dateDiscovered());
        incident.setDateReported(request.dateReported());
        incident.setBusinessUnit(resolveBusinessUnit(request.businessUnit()));
        incident.setLocationChannel(request.locationChannel());
        incident.setCategory(request.category());
        incident.setEventType(request.eventType());
        incident.setTitle(request.title());
        incident.setDescription(request.description());
        incident.setLikelihood(request.likelihood());
        incident.setImpact(request.impact());
        incident.setCustomersAffected(request.customersAffected());
        incident.setServiceDowntimeMinutes(request.serviceDowntimeMinutes());
        incident.setGrossLoss(request.grossLoss() != null ? request.grossLoss() : BigDecimal.ZERO);
        incident.setRecovery(request.recovery() != null ? request.recovery() : BigDecimal.ZERO);
        if (request.dataBreach() != null) incident.setDataBreach(request.dataBreach());
        if (request.regulatorNotifiable() != null) incident.setRegulatorNotifiable(request.regulatorNotifiable());
        incident.setBnrNotificationDate(request.bnrNotificationDate());
        incident.setImmediateContainment(request.immediateContainment());
        incident.setRootCause(request.rootCause());
        incident.setCorrectiveAction(request.correctiveAction());
        incident.setTargetClosureDate(request.targetClosureDate());
        incident.setEvidenceReference(request.evidenceReference());
        incident.setReportedBy(reportedBy);
        incident.setIncidentOwner(request.incidentOwner());
        incident.setLessonsLearned(request.lessonsLearned());
        incident.setStatus(IncidentStatus.OPEN);

        if (request.actionOwnerId() != null) {
            incident.setActionOwner(resolvePerson(request.actionOwnerId(), "actionOwnerId"));
        }

        applyDerivedRiskFields(incident);

        Incident saved = incidentRepository.save(incident);

        IncidentStatusChange initial = new IncidentStatusChange();
        initial.setIncident(saved);
        initial.setFromStatus(null);
        initial.setToStatus(IncidentStatus.OPEN);
        initial.setChangedBy(reportedBy);
        initial.setNote("Incident reported.");
        incidentStatusChangeRepository.save(initial);
        saved.getStatusChanges().add(initial);

        notificationService.notifyIncidentReported(saved, reportedBy);
        if (saved.getActionOwner() != null) {
            notificationService.notifyIncidentActionOwnerAssigned(saved, reportedBy);
        }

        return incidentMapper.toDetailResponse(saved);
    }

    @Override
    public IncidentDetailResponse getIncidentById(Long id) {
        return incidentMapper.toDetailResponse(findIncident(id));
    }

    @Override
    public Page<IncidentListResponse> getAllIncidents(
            IncidentStatus status, IncidentSeverity severity, IncidentCategory category,
            String businessUnit, LocalDate from, LocalDate to, String q, Pageable pageable) {
        // Built here, not with CONCAT inside the query itself — see IncidentRepository.search's
        // Javadoc for why a null :q through LOWER(CONCAT(...)) broke on Postgres.
        String qPattern = (q == null || q.isBlank()) ? null : "%" + q.trim().toLowerCase() + "%";
        String unit = (businessUnit == null || businessUnit.isBlank()) ? null : businessUnit.trim();
        String legacyUnit = unit == null ? null : unit.toUpperCase().replace(' ', '_');
        return incidentRepository.search(status, severity, category, unit, legacyUnit, from, to, qPattern, pageable)
                .map(incidentMapper::toListResponse);
    }

    @Override
    public IncidentDetailResponse updateIncident(Long id, UpdateIncidentRequest request) {
        Person actor = personRepository.findById(request.changedById())
                .orElseThrow(() -> new ResourceNotFoundException("changedById not found"));
        if (!Role.isAtLeastDirector(actor.getRole())) {
            throw new ForbiddenActionException("Only a Director, Executive, or Super Admin can edit an incident.");
        }

        Incident incident = findIncident(id);
        Person previousOwner = incident.getActionOwner();

        if (request.dateOccurred() != null) incident.setDateOccurred(request.dateOccurred());
        incident.setTimeOccurred(request.timeOccurred());
        if (request.dateDiscovered() != null) incident.setDateDiscovered(request.dateDiscovered());
        if (request.dateReported() != null) incident.setDateReported(request.dateReported());
        if (request.businessUnit() != null) incident.setBusinessUnit(resolveBusinessUnit(request.businessUnit()));
        incident.setLocationChannel(request.locationChannel());
        if (request.category() != null) incident.setCategory(request.category());
        incident.setEventType(request.eventType());
        incident.setTitle(request.title());
        incident.setDescription(request.description());
        incident.setLikelihood(request.likelihood());
        incident.setImpact(request.impact());
        incident.setCustomersAffected(request.customersAffected());
        incident.setServiceDowntimeMinutes(request.serviceDowntimeMinutes());
        incident.setGrossLoss(request.grossLoss() != null ? request.grossLoss() : BigDecimal.ZERO);
        incident.setRecovery(request.recovery() != null ? request.recovery() : BigDecimal.ZERO);
        if (request.dataBreach() != null) incident.setDataBreach(request.dataBreach());
        if (request.regulatorNotifiable() != null) incident.setRegulatorNotifiable(request.regulatorNotifiable());
        incident.setBnrNotificationDate(request.bnrNotificationDate());
        incident.setImmediateContainment(request.immediateContainment());
        incident.setRootCause(request.rootCause());
        incident.setCorrectiveAction(request.correctiveAction());
        incident.setTargetClosureDate(request.targetClosureDate());
        incident.setActualClosureDate(request.actualClosureDate());
        incident.setEvidenceReference(request.evidenceReference());
        incident.setIncidentOwner(request.incidentOwner());
        if (request.riskReview() != null) incident.setRiskReview(request.riskReview());
        if (request.complianceReview() != null) incident.setComplianceReview(request.complianceReview());
        incident.setLessonsLearned(request.lessonsLearned());

        if (request.actionOwnerId() == null) {
            incident.setActionOwner(null);
        } else {
            incident.setActionOwner(resolvePerson(request.actionOwnerId(), "actionOwnerId"));
        }

        applyDerivedRiskFields(incident);

        Incident saved = incidentRepository.save(incident);

        boolean ownerChanged = saved.getActionOwner() != null
                && (previousOwner == null || !previousOwner.getId().equals(saved.getActionOwner().getId()));
        if (ownerChanged) {
            notificationService.notifyIncidentActionOwnerAssigned(saved, actor);
        }

        return incidentMapper.toDetailResponse(saved);
    }

    @Override
    public IncidentDetailResponse changeStatus(Long id, ChangeIncidentStatusRequest request) {
        Person actor = personRepository.findById(request.changedById())
                .orElseThrow(() -> new ResourceNotFoundException("changedById not found"));
        if (!Role.isAtLeastDirector(actor.getRole())) {
            throw new ForbiddenActionException("Only a Director, Executive, or Super Admin can change an incident's status.");
        }

        Incident incident = findIncident(id);
        IncidentStatus from = incident.getStatus();
        IncidentStatus to = request.newStatus();

        if (to == IncidentStatus.CLOSED) {
            requireClosureReadiness(incident);
            if (incident.getActualClosureDate() == null) {
                incident.setActualClosureDate(LocalDate.now());
            }
        }

        incident.setStatus(to);
        Incident saved = incidentRepository.save(incident);

        IncidentStatusChange change = new IncidentStatusChange();
        change.setIncident(saved);
        change.setFromStatus(from);
        change.setToStatus(to);
        change.setChangedBy(actor);
        change.setNote(request.note());
        incidentStatusChangeRepository.save(change);
        saved.getStatusChanges().add(change);

        return incidentMapper.toDetailResponse(saved);
    }

    @Override
    public IncidentDashboardResponse getDashboard() {
        List<Incident> all = incidentRepository.findAll();

        long total = all.size();
        long openOrMonitoring = all.stream().filter(i -> OPEN_OR_MONITORING.contains(i.getStatus())).count();
        long criticalOrHigh = all.stream().filter(i -> HIGH_TIER.contains(i.getSeverity())).count();
        long overdueActions = all.stream()
                .filter(i -> i.getStatus() != IncidentStatus.CLOSED
                        && i.getTargetClosureDate() != null
                        && i.getTargetClosureDate().isBefore(LocalDate.now()))
                .count();

        BigDecimal grossTotal = all.stream().map(Incident::getGrossLoss).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal recoveryTotal = all.stream().map(Incident::getRecovery).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal netTotal = all.stream().map(Incident::getNetLoss).reduce(BigDecimal.ZERO, BigDecimal::add);

        long regulatorNotifiable = all.stream()
                .filter(i -> i.getRegulatorNotifiable() == com.throughline.taskmanagement.enums.RegulatorNotifiableStatus.YES)
                .count();

        List<IncidentCountResponse> statusBreakdown = countBy(all, i -> i.getStatus().name());
        List<IncidentCountResponse> severityBreakdown = countBy(
                all.stream().filter(i -> i.getSeverity() != null).toList(), i -> i.getSeverity().name());
        List<IncidentCountResponse> categoryBreakdown = countBy(all, i -> i.getCategory().name());

        Map<String, Long> byMonth = new TreeMap<>();
        DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("yyyy-MM");
        for (Incident i : all) {
            String key = i.getDateOccurred().format(monthFmt);
            byMonth.merge(key, 1L, Long::sum);
        }
        List<IncidentMonthlyCountResponse> monthlyTrend = new ArrayList<>();
        byMonth.forEach((month, count) -> monthlyTrend.add(new IncidentMonthlyCountResponse(month, count)));

        List<Incident> closed = all.stream().filter(i -> i.getStatus() == IncidentStatus.CLOSED).toList();
        long closedLate = closed.stream()
                .filter(i -> i.getTargetClosureDate() != null && i.getActualClosureDate() != null
                        && i.getActualClosureDate().isAfter(i.getTargetClosureDate()))
                .count();
        double slaComplianceRate = closed.isEmpty() ? 100.0
                : (100.0 * (closed.size() - closedLate) / closed.size());

        return new IncidentDashboardResponse(
                total, openOrMonitoring, criticalOrHigh, overdueActions,
                grossTotal, recoveryTotal, netTotal, regulatorNotifiable,
                statusBreakdown, severityBreakdown, categoryBreakdown, monthlyTrend,
                closed.size(), closedLate, slaComplianceRate
        );
    }

    // --- helpers ---

    /** Business Unit must be a Department that currently exists (matched case-insensitively, stored under
     *  its canonical name) or the catch-all "Other" carried over from the Excel's list. */
    private String resolveBusinessUnit(String requested) {
        String name = requested == null ? "" : requested.trim();
        if (name.equalsIgnoreCase("Other")) {
            return "Other";
        }
        return departmentRepository.findAll().stream()
                .map(department -> department.getName())
                .filter(existing -> existing.equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new InvalidAssignmentException(
                        "Business unit must be one of the existing departments (or \"Other\")."));
    }

    /** Checked only at creation, not on every update — an incident already past its target date (an overdue
     *  one) must stay editable without being forced to bump the date just to save an unrelated field. */
    private void requireTargetClosureDateNotInPast(LocalDate targetClosureDate) {
        if (targetClosureDate != null && targetClosureDate.isBefore(LocalDate.now())) {
            throw new InvalidAssignmentException("Target closure date can't be in the past.");
        }
    }

    private Incident findIncident(Long id) {
        return incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found"));
    }

    private Person resolvePerson(Long id, String fieldName) {
        return personRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(fieldName + " not found"));
    }

    /** Recomputes inherentScore/severity/netLoss from the entity's own current fields —
     *  called on every create/update so these three are never trusted from a request body. */
    private void applyDerivedRiskFields(Incident incident) {
        Integer inherentScore = incidentMapper.computeInherentScore(incident.getLikelihood(), incident.getImpact());
        incident.setInherentScore(inherentScore);
        incident.setSeverity(incidentMapper.computeSeverity(inherentScore));
        BigDecimal net = incident.getGrossLoss().subtract(incident.getRecovery());
        incident.setNetLoss(net.max(BigDecimal.ZERO));
    }

    /** Mirrors IncidentMapper's own closureBlockers computation — kept in sync deliberately
     *  (see that method's Javadoc) so what the UI shows as "why you can't close this yet"
     *  matches exactly what actually blocks the request. */
    private void requireClosureReadiness(Incident incident) {
        List<String> blockers = new ArrayList<>();
        if (incident.getRootCause() == null || incident.getRootCause().isBlank()) {
            blockers.add("root cause");
        }
        if (incident.getCorrectiveAction() == null || incident.getCorrectiveAction().isBlank()) {
            blockers.add("corrective/preventive action");
        }
        boolean needsReview = HIGH_TIER.contains(incident.getSeverity())
                || incident.getRegulatorNotifiable() == com.throughline.taskmanagement.enums.RegulatorNotifiableStatus.YES
                || incident.getRegulatorNotifiable() == com.throughline.taskmanagement.enums.RegulatorNotifiableStatus.ASSESS;
        if (needsReview) {
            if (incident.getRiskReview() != com.throughline.taskmanagement.enums.IncidentReviewStatus.COMPLETED) {
                blockers.add("risk review");
            }
            if (incident.getComplianceReview() != com.throughline.taskmanagement.enums.IncidentReviewStatus.COMPLETED) {
                blockers.add("compliance review");
            }
        }
        if (!blockers.isEmpty()) {
            throw new ForbiddenActionException("Cannot close this incident yet — missing: " + String.join(", ", blockers) + ".");
        }
    }

    private String generateIncidentCode() {
        String prefix = "INC-" + LocalDate.now().getYear() + "-";
        long nextSequence = incidentRepository.countByIncidentCodeStartingWith(prefix) + 1;
        return prefix + String.format("%03d", nextSequence);
    }

    private List<IncidentCountResponse> countBy(List<Incident> incidents, java.util.function.Function<Incident, String> classifier) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (Incident incident : incidents) {
            counts.merge(classifier.apply(incident), 1L, Long::sum);
        }
        List<IncidentCountResponse> result = new ArrayList<>();
        counts.forEach((label, count) -> result.add(new IncidentCountResponse(label, count)));
        return result;
    }
}
