package com.throughline.taskmanagement.service;

import com.throughline.taskmanagement.dto.response.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface DashboardService {
    DashboardOverviewResponse getOverview();
    List<StatusMixResponse> getStatusMix();
    List<ProgressPointResponse> getProgressOverTime(LocalDate from, LocalDate to);
    List<TeamLeaderboardResponse> getTeamLeaderboard();
    List<PersonSummaryResponse> getPeopleSummary();
    GlobalSearchResponse globalSearch(String q);

    /** The Director's Dashboard default view: only the top-level tasks THIS Director
     *  created, not the whole org's tasks. */
    Page<TaskListResponse> getDirectorTasks(Long directorId, Pageable pageable);

    /** The Executive's Dashboard view: every CRITICAL-severity task org-wide plus every task
     *  an Executive/Super Admin personally assigned, not just the ones this particular
     *  Executive created and not scoped to top-level depth (see TaskRepository.
     *  findBySeverityOrAssignedByRoleIn). Executive-or-above only (Super Admin sees the
     *  literal same view, not a separate lookalike). */
    Page<TaskListResponse> getExecutiveTasks(Long viewerId, Pageable pageable);

    /** The Executive Dashboard's department traffic-light roll-up — one row per department,
     *  replacing the task-card list an Executive used to see. Same viewer-gating pattern as
     *  getExecutiveTasks (Executive-or-above only). */
    List<DepartmentHealthResponse> getExecutiveDepartmentHealth(Long viewerId);

    /** The Executive Dashboard's four org-health KPI tiles. Same viewer-gating pattern as
     *  getExecutiveTasks. */
    ExecutiveKpiResponse getExecutiveKpis(Long viewerId);
}
