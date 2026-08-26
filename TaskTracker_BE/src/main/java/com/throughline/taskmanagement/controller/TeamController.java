package com.throughline.taskmanagement.controller;

import com.throughline.taskmanagement.dto.request.CreateTeamRequest;
import com.throughline.taskmanagement.dto.response.TaskListResponse;
import com.throughline.taskmanagement.dto.response.TeamResponse;
import com.throughline.taskmanagement.dto.response.TeamStatisticsResponse;
import com.throughline.taskmanagement.service.TeamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

    @GetMapping("/{id}")
    public ResponseEntity<TeamResponse> getTeamById(@PathVariable Long id) {
        return ResponseEntity.ok(teamService.getTeamById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TeamResponse> updateTeam(
            @PathVariable Long id, 
            @Valid @RequestBody CreateTeamRequest request) {
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

    @PutMapping("/{id}/leader/{personId}")
    public ResponseEntity<TeamResponse> setTeamLeader(
            @PathVariable Long id, 
            @PathVariable Long personId) {
        return ResponseEntity.ok(teamService.setTeamLeader(id, personId));
    }

    @PostMapping("/{id}/members/{personId}")
    public ResponseEntity<TeamResponse> addMember(
            @PathVariable Long id, 
            @PathVariable Long personId) {
        return ResponseEntity.ok(teamService.addMember(id, personId));
    }

    @DeleteMapping("/{id}/members/{personId}")
    public ResponseEntity<TeamResponse> removeMember(
            @PathVariable Long id, 
            @PathVariable Long personId) {
        return ResponseEntity.ok(teamService.removeMember(id, personId));
    }
}
