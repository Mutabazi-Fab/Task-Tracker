package com.throughline.taskmanagement.controller;

import com.throughline.taskmanagement.dto.response.*;
import com.throughline.taskmanagement.security.CurrentPersonResolver;
import com.throughline.taskmanagement.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final CurrentPersonResolver currentPersonResolver;

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
     *  created, not the whole org's tasks. directorId is never accepted from the client —
     *  it's always the caller's own real, logged-in identity, so a Member can't view a
     *  Director's "my initiatives" list just by passing that Director's id. */
    @GetMapping("/director/tasks")
    public ResponseEntity<Page<TaskListResponse>> getDirectorTasks(Pageable pageable, Authentication authentication) {
        Long directorId = currentPersonResolver.resolveId(authentication);
        return ResponseEntity.ok(dashboardService.getDirectorTasks(directorId, pageable));
    }
}
