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
     *  an Executive/Super Admin personally assigned, any depth (see TaskRepository.
     *  findBySeverityOrAssignedByRoleIn). Executive-or-above only. */
    Page<TaskListResponse> getExecutiveTasks(Long viewerId, Pageable pageable);

    /** The Director Dashboard's "Critical & CEO-assigned" panel — the department-scoped
     *  equivalent of getExecutiveTasks: every HIGH/CRITICAL-severity task within this
     *  Director's own department, plus every task in it an Executive/Super Admin personally
     *  assigned. Director-or-above only; empty if the Director belongs to no department. */
    Page<TaskListResponse> getDirectorCriticalAndCeoAssignedTasks(Long directorId, Pageable pageable);

    /** The Executive Dashboard's department traffic-light roll-up — one row per department.
     *  Executive-or-above only. */
    List<DepartmentHealthResponse> getExecutiveDepartmentHealth(Long viewerId);

    /** The Executive Dashboard's four org-health KPI tiles. */
    ExecutiveKpiResponse getExecutiveKpis(Long viewerId);
}
