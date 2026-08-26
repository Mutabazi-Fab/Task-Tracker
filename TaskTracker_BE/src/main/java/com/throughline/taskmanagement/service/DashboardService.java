package com.throughline.taskmanagement.service;

import com.throughline.taskmanagement.dto.response.*;

import java.time.LocalDate;
import java.util.List;

public interface DashboardService {
    DashboardOverviewResponse getOverview();
    List<StatusMixResponse> getStatusMix();
    List<ProgressPointResponse> getProgressOverTime(LocalDate from, LocalDate to);
    List<TeamLeaderboardResponse> getTeamLeaderboard();
    List<PersonSummaryResponse> getPeopleSummary();
    GlobalSearchResponse globalSearch(String q);
}
