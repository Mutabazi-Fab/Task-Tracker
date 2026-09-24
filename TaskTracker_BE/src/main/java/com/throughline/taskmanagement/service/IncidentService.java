package com.throughline.taskmanagement.service;

import com.throughline.taskmanagement.dto.request.ChangeIncidentStatusRequest;
import com.throughline.taskmanagement.dto.request.CreateIncidentRequest;
import com.throughline.taskmanagement.dto.request.UpdateIncidentRequest;
import com.throughline.taskmanagement.dto.response.IncidentDashboardResponse;
import com.throughline.taskmanagement.dto.response.IncidentDetailResponse;
import com.throughline.taskmanagement.dto.response.IncidentListResponse;

import com.throughline.taskmanagement.enums.IncidentCategory;
import com.throughline.taskmanagement.enums.IncidentSeverity;
import com.throughline.taskmanagement.enums.IncidentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface IncidentService {

    /** Director/Executive/Super Admin only (Role.isAtLeastDirector) — enforced here.
     *  reportedById is always the caller's own real, JWT-resolved identity. */
    IncidentDetailResponse createIncident(CreateIncidentRequest request);

    /** Fails with a Forbidden error unless the viewer may see it — see IncidentAccessPolicy. */
    IncidentDetailResponse getIncidentById(Long id, Long viewerId);

    /** Every filter is optional. q searches title and incident code. */
    Page<IncidentListResponse> getAllIncidents(
            IncidentStatus status, IncidentSeverity severity, IncidentCategory category,
            String businessUnit, LocalDate from, LocalDate to, String q, Long viewerId, Pageable pageable);

    /** Director/Executive/Super Admin only, same tier as creating one. changedById is
     *  always the caller's own real, JWT-resolved identity. */
    IncidentDetailResponse updateIncident(Long id, UpdateIncidentRequest request);

    /** Records the transition in IncidentStatusChange. */
    IncidentDetailResponse changeStatus(Long id, ChangeIncidentStatusRequest request);

    /** Backs the Incident Management dashboard's KPI tiles, breakdowns and trend. */
    /** Counts only what this viewer may see (see IncidentAccessPolicy). */
    IncidentDashboardResponse getDashboard(Long viewerId);
}
