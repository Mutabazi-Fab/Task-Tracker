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
}
