package com.throughline.taskmanagement.controller;

import com.throughline.taskmanagement.dto.response.*;
import com.throughline.taskmanagement.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/overview")
    public ResponseEntity<DashboardOverviewResponse> getOverview() {
        return ResponseEntity.ok(dashboardService.getOverview());
    }

    @GetMapping("/status-mix")
    public ResponseEntity<List<StatusMixResponse>> getStatusMix() {
        return ResponseEntity.ok(dashboardService.getStatusMix());
    }

    @GetMapping("/progress-over-time")
    public ResponseEntity<List<ProgressPointResponse>> getProgressOverTime(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(dashboardService.getProgressOverTime(from, to));
    }

    @GetMapping("/team-leaderboard")
    public ResponseEntity<List<TeamLeaderboardResponse>> getTeamLeaderboard() {
        return ResponseEntity.ok(dashboardService.getTeamLeaderboard());
    }

    @GetMapping("/people-summary")
    public ResponseEntity<List<PersonSummaryResponse>> getPeopleSummary() {
        return ResponseEntity.ok(dashboardService.getPeopleSummary());
    }

    @GetMapping("/search")
    public ResponseEntity<GlobalSearchResponse> globalSearch(@RequestParam String q) {
        return ResponseEntity.ok(dashboardService.globalSearch(q));
    }

    /** The Director's Dashboard default view: only the top-level tasks THIS Director
     *  created, not the whole org's tasks. directorId is passed explicitly (same pattern
     *  as the rest of the app until Phase 7 wires a real "current user"). */
    @GetMapping("/director/tasks")
    public ResponseEntity<Page<TaskListResponse>> getDirectorTasks(
            @RequestParam Long directorId, Pageable pageable) {
        return ResponseEntity.ok(dashboardService.getDirectorTasks(directorId, pageable));
    }
}
