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
import com.throughline.taskmanagement.model.Person;
import com.throughline.taskmanagement.security.CurrentPersonResolver;
import com.throughline.taskmanagement.service.TeamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Every "who's doing this" field (createdById/changedById) is now re-derived from the
 * caller's actual login (see CurrentPersonResolver) rather than trusted from the request
 * body — a request can no longer claim to be a Director by just putting a Director's id
 * in the JSON. The rest of each request is passed through unchanged, so TeamService's own
 * role checks (already written against whatever id they're given) become trustworthy
 * without needing to change at all.
 */
@RestController
@RequestMapping("/api/v1/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;
    private final CurrentPersonResolver currentPersonResolver;

    @PostMapping
    public ResponseEntity<TeamResponse> createTeam(@Valid @RequestBody CreateTeamRequest request, Authentication authentication) {
        Long actorId = currentPersonResolver.resolveId(authentication);
        CreateTeamRequest verified = new CreateTeamRequest(request.name(), actorId, request.leaderId(), request.memberIds());
        return new ResponseEntity<>(teamService.createTeam(verified), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<TeamResponse>> getAllTeams() {
        return ResponseEntity.ok(teamService.getAllTeams());
    }

    /** Recent membership activity across ALL teams — the Director's cross-team audit feed. */
    @GetMapping("/activity")
    public ResponseEntity<Page<TeamMembershipChangeResponse>> getAllMembershipActivity(Pageable pageable, Authentication authentication) {
        Person viewer = currentPersonResolver.resolve(authentication);
        return ResponseEntity.ok(teamService.getAllMembershipActivity(viewer.getId(), pageable));
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

    /** Director/Super Admin, or a member of this specific team — everyone else is
     *  forbidden, not just shown less by the frontend. */
    @GetMapping("/{id}/statistics")
    public ResponseEntity<TeamStatisticsResponse> getTeamStatistics(@PathVariable Long id, Authentication authentication) {
        Long viewerId = currentPersonResolver.resolveId(authentication);
        return ResponseEntity.ok(teamService.getTeamStatistics(id, viewerId));
    }

    @GetMapping("/{id}/tasks")
    public ResponseEntity<List<TaskListResponse>> getTeamTasks(@PathVariable Long id, Authentication authentication) {
        Long viewerId = currentPersonResolver.resolveId(authentication);
        return ResponseEntity.ok(teamService.getTeamTasks(id, viewerId));
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<List<TeamMemberResponse>> getTeamMembers(@PathVariable Long id, Authentication authentication) {
        Long viewerId = currentPersonResolver.resolveId(authentication);
        return ResponseEntity.ok(teamService.getTeamMembers(id, viewerId));
    }

    @PutMapping("/{id}/leader/{personId}")
    public ResponseEntity<TeamResponse> setTeamLeader(
            @PathVariable Long id,
            @PathVariable Long personId,
            @Valid @RequestBody SetTeamLeaderRequest request,
            Authentication authentication) {
        Long actorId = currentPersonResolver.resolveId(authentication);
        SetTeamLeaderRequest verified = new SetTeamLeaderRequest(actorId);
        return ResponseEntity.ok(teamService.setTeamLeader(id, personId, verified));
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<TeamResponse> addMember(
            @PathVariable Long id,
            @Valid @RequestBody AddTeamMemberRequest request,
            Authentication authentication) {
        Long actorId = currentPersonResolver.resolveId(authentication);
        AddTeamMemberRequest verified = new AddTeamMemberRequest(request.personId(), actorId, request.reason());
        return ResponseEntity.ok(teamService.addMember(id, verified));
    }

    @DeleteMapping("/{id}/members/{personId}")
    public ResponseEntity<TeamResponse> removeMember(
            @PathVariable Long id,
            @PathVariable Long personId,
            @Valid @RequestBody RemoveTeamMemberRequest request,
            Authentication authentication) {
        Long actorId = currentPersonResolver.resolveId(authentication);
        RemoveTeamMemberRequest verified = new RemoveTeamMemberRequest(actorId, request.reason());
        return ResponseEntity.ok(teamService.removeMember(id, personId, verified));
    }

    @GetMapping("/{id}/membership-history")
    public ResponseEntity<Page<TeamMembershipChangeResponse>> getMembershipHistory(
            @PathVariable Long id, Pageable pageable, Authentication authentication) {
        Long viewerId = currentPersonResolver.resolveId(authentication);
        return ResponseEntity.ok(teamService.getMembershipHistory(id, viewerId, pageable));
    }
}
