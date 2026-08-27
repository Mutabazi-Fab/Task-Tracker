package com.throughline.taskmanagement.controller;

import com.throughline.taskmanagement.dto.request.AddTeamMemberRequest;
import com.throughline.taskmanagement.dto.request.CreateTeamRequest;
import com.throughline.taskmanagement.dto.request.RemoveTeamMemberRequest;
import com.throughline.taskmanagement.dto.request.SetTeamLeaderRequest;
import com.throughline.taskmanagement.dto.request.UpdateTeamRequest;
import com.throughline.taskmanagement.dto.response.TaskListResponse;
import com.throughline.taskmanagement.dto.response.TeamMemberResponse;
import com.throughline.taskmanagement.dto.response.TeamMembershipChangeResponse;
import com.throughline.taskmanagement.dto.response.TeamResponse;
import com.throughline.taskmanagement.dto.response.TeamStatisticsResponse;
import com.throughline.taskmanagement.service.TeamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @PostMapping
    public ResponseEntity<TeamResponse> createTeam(@Valid @RequestBody CreateTeamRequest request) {
        return new ResponseEntity<>(teamService.createTeam(request), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<TeamResponse>> getAllTeams() {
        return ResponseEntity.ok(teamService.getAllTeams());
    }

    /** Recent membership activity across ALL teams — the Director's cross-team audit feed. */
    @GetMapping("/activity")
    public ResponseEntity<Page<TeamMembershipChangeResponse>> getAllMembershipActivity(Pageable pageable) {
        return ResponseEntity.ok(teamService.getAllMembershipActivity(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TeamResponse> getTeamById(@PathVariable Long id) {
        return ResponseEntity.ok(teamService.getTeamById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TeamResponse> updateTeam(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTeamRequest request) {
        return ResponseEntity.ok(teamService.updateTeam(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTeam(@PathVariable Long id) {
        teamService.deleteTeam(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/statistics")
    public ResponseEntity<TeamStatisticsResponse> getTeamStatistics(@PathVariable Long id) {
        return ResponseEntity.ok(teamService.getTeamStatistics(id));
    }

    @GetMapping("/{id}/tasks")
    public ResponseEntity<List<TaskListResponse>> getTeamTasks(@PathVariable Long id) {
        return ResponseEntity.ok(teamService.getTeamTasks(id));
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<List<TeamMemberResponse>> getTeamMembers(@PathVariable Long id) {
        return ResponseEntity.ok(teamService.getTeamMembers(id));
    }

    @PutMapping("/{id}/leader/{personId}")
    public ResponseEntity<TeamResponse> setTeamLeader(
            @PathVariable Long id,
            @PathVariable Long personId,
            @Valid @RequestBody SetTeamLeaderRequest request) {
        return ResponseEntity.ok(teamService.setTeamLeader(id, personId, request));
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<TeamResponse> addMember(
            @PathVariable Long id,
            @Valid @RequestBody AddTeamMemberRequest request) {
        return ResponseEntity.ok(teamService.addMember(id, request));
    }

    @DeleteMapping("/{id}/members/{personId}")
    public ResponseEntity<TeamResponse> removeMember(
            @PathVariable Long id,
            @PathVariable Long personId,
            @Valid @RequestBody RemoveTeamMemberRequest request) {
        return ResponseEntity.ok(teamService.removeMember(id, personId, request));
    }

    @GetMapping("/{id}/membership-history")
    public ResponseEntity<Page<TeamMembershipChangeResponse>> getMembershipHistory(@PathVariable Long id, Pageable pageable) {
        return ResponseEntity.ok(teamService.getMembershipHistory(id, pageable));
    }
}
