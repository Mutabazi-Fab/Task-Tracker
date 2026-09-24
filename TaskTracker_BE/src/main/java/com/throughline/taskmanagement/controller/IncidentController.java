package com.throughline.taskmanagement.controller;

import com.throughline.taskmanagement.dto.request.ChangeIncidentStatusRequest;
import com.throughline.taskmanagement.dto.request.CreateIncidentRequest;
import com.throughline.taskmanagement.dto.request.UpdateIncidentRequest;
import com.throughline.taskmanagement.dto.response.IncidentDashboardResponse;
import com.throughline.taskmanagement.dto.response.IncidentDetailResponse;
import com.throughline.taskmanagement.dto.response.IncidentListResponse;

import com.throughline.taskmanagement.enums.IncidentCategory;
import com.throughline.taskmanagement.enums.IncidentSeverity;
import com.throughline.taskmanagement.enums.IncidentStatus;
import com.throughline.taskmanagement.security.CurrentPersonResolver;
import com.throughline.taskmanagement.service.IncidentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Every "who's doing this" field (reportedById/changedById) is re-derived from the caller's
 * actual login (CurrentPersonResolver), never trusted from the request body — same rule as
 * TaskController.
 */
@RestController
@RequestMapping("/api/v1/incidents")
@RequiredArgsConstructor
public class IncidentController {

    private final IncidentService incidentService;
    private final CurrentPersonResolver currentPersonResolver;

    @PostMapping
    public ResponseEntity<IncidentDetailResponse> createIncident(
            @Valid @RequestBody CreateIncidentRequest request, Authentication authentication) {
        Long actorId = currentPersonResolver.resolveId(authentication);
        CreateIncidentRequest verified = new CreateIncidentRequest(
                request.dateOccurred(), request.timeOccurred(), request.dateDiscovered(), request.dateReported(),
                request.businessUnit(), request.locationChannel(), request.category(), request.eventType(),
                request.title(), request.description(), request.likelihood(), request.impact(),
                request.customersAffected(), request.serviceDowntimeMinutes(), request.grossLoss(), request.recovery(),
                request.dataBreach(), request.regulatorNotifiable(), request.bnrNotificationDate(),
                request.immediateContainment(), request.rootCause(), request.correctiveAction(),
                request.actionOwnerId(), request.targetClosureDate(), request.evidenceReference(),
                actorId, request.incidentOwner(), request.lessonsLearned());
        return new ResponseEntity<>(incidentService.createIncident(verified), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<IncidentDetailResponse> getIncidentById(@PathVariable Long id) {
        return ResponseEntity.ok(incidentService.getIncidentById(id));
    }

    @GetMapping
    public ResponseEntity<Page<IncidentListResponse>> getAllIncidents(
            @RequestParam(required = false) IncidentStatus status,
            @RequestParam(required = false) IncidentSeverity severity,
            @RequestParam(required = false) IncidentCategory category,
            @RequestParam(required = false) String businessUnit,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String q,
            Pageable pageable) {
        return ResponseEntity.ok(incidentService.getAllIncidents(status, severity, category, businessUnit, from, to, q, pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<IncidentDetailResponse> updateIncident(
            @PathVariable Long id, @Valid @RequestBody UpdateIncidentRequest request, Authentication authentication) {
        Long actorId = currentPersonResolver.resolveId(authentication);
        UpdateIncidentRequest verified = new UpdateIncidentRequest(
                request.dateOccurred(), request.timeOccurred(), request.dateDiscovered(), request.dateReported(),
                request.businessUnit(), request.locationChannel(), request.category(), request.eventType(),
                request.title(), request.description(), request.likelihood(), request.impact(),
                request.customersAffected(), request.serviceDowntimeMinutes(), request.grossLoss(), request.recovery(),
                request.dataBreach(), request.regulatorNotifiable(), request.bnrNotificationDate(),
                request.immediateContainment(), request.rootCause(), request.correctiveAction(),
                request.actionOwnerId(), request.targetClosureDate(), request.actualClosureDate(),
                request.evidenceReference(), request.incidentOwner(), request.riskReview(), request.complianceReview(),
                request.lessonsLearned(), actorId);
        return ResponseEntity.ok(incidentService.updateIncident(id, verified));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<IncidentDetailResponse> changeStatus(
            @PathVariable Long id, @Valid @RequestBody ChangeIncidentStatusRequest request, Authentication authentication) {
        Long actorId = currentPersonResolver.resolveId(authentication);
        ChangeIncidentStatusRequest verified = new ChangeIncidentStatusRequest(request.newStatus(), request.note(), actorId);
        return ResponseEntity.ok(incidentService.changeStatus(id, verified));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<IncidentDashboardResponse> getDashboard() {
        return ResponseEntity.ok(incidentService.getDashboard());
    }
}
