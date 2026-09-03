package com.throughline.taskmanagement.controller;

import com.throughline.taskmanagement.dto.request.ChangeRoleRequest;
import com.throughline.taskmanagement.dto.request.CreatePersonRequest;
import com.throughline.taskmanagement.dto.request.SendPasswordResetRequest;
import com.throughline.taskmanagement.dto.request.SetAccountActiveRequest;
import com.throughline.taskmanagement.dto.response.AccountStatusChangeResponse;
import com.throughline.taskmanagement.dto.response.PersonResponse;
import com.throughline.taskmanagement.dto.response.PersonStatisticsResponse;
import com.throughline.taskmanagement.dto.response.PersonTaskHistoryResponse;
import com.throughline.taskmanagement.dto.response.RoleChangeResponse;
import com.throughline.taskmanagement.security.CurrentPersonResolver;
import com.throughline.taskmanagement.service.PersonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Every "who's doing this" field (createdById/changedById/requesterId) is re-derived from
 * the caller's actual login (CurrentPersonResolver), not trusted from the request — see
 * TeamController for the same pattern. getAllPeople is likewise scoped to the caller's
 * real role: a Director/Super Admin sees everyone, anyone else sees only their teammates.
 */
@RestController
@RequestMapping("/api/v1/people")
@RequiredArgsConstructor
public class PersonController {

    private final PersonService personService;
    private final CurrentPersonResolver currentPersonResolver;

    @PostMapping
    public ResponseEntity<PersonResponse> createPerson(@Valid @RequestBody CreatePersonRequest request, Authentication authentication) {
        Long actorId = currentPersonResolver.resolveId(authentication);
        CreatePersonRequest verified = new CreatePersonRequest(
                request.fullName(), request.email(), request.jobTitle(), request.rank(), actorId, request.role());
        return new ResponseEntity<>(personService.createPerson(verified), HttpStatus.CREATED);
    }

    /** A Director/Super Admin gets everyone; anyone else gets only people who share at
     *  least one team with them (empty if they belong to no team). */
    @GetMapping
    public ResponseEntity<Page<PersonResponse>> getAllPeople(Pageable pageable, Authentication authentication) {
        Long viewerId = currentPersonResolver.resolveId(authentication);
        return ResponseEntity.ok(personService.getAllPeople(viewerId, pageable));
    }

    /** Director/Super Admin, or the caller viewing themself or a teammate — anyone else
     *  gets a 403, not just a frontend that declines to link there. */
    @GetMapping("/{id}")
    public ResponseEntity<PersonResponse> getPersonById(@PathVariable Long id, Authentication authentication) {
        Long viewerId = currentPersonResolver.resolveId(authentication);
        return ResponseEntity.ok(personService.getPersonById(id, viewerId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PersonResponse> updatePerson(
            @PathVariable Long id,
            @Valid @RequestBody CreatePersonRequest request) {
        return ResponseEntity.ok(personService.updatePerson(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePerson(@PathVariable Long id) {
        personService.deletePerson(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/statistics")
    public ResponseEntity<PersonStatisticsResponse> getPersonStatistics(@PathVariable Long id, Authentication authentication) {
        Long viewerId = currentPersonResolver.resolveId(authentication);
        return ResponseEntity.ok(personService.getPersonStatistics(id, viewerId));
    }

    @GetMapping("/{id}/tasks")
    public ResponseEntity<Page<PersonTaskHistoryResponse>> getPersonTaskHistory(
            @PathVariable Long id, Pageable pageable, Authentication authentication) {
        Long viewerId = currentPersonResolver.resolveId(authentication);
        return ResponseEntity.ok(personService.getPersonTaskHistory(id, viewerId, pageable));
    }

    /** Super-Admin-only — promotes/demotes between Member, Director, and Super Admin. */
    @PutMapping("/{id}/role")
    public ResponseEntity<PersonResponse> changeRole(
            @PathVariable Long id, @Valid @RequestBody ChangeRoleRequest request, Authentication authentication) {
        Long actorId = currentPersonResolver.resolveId(authentication);
        ChangeRoleRequest verified = new ChangeRoleRequest(request.newRole(), actorId, request.reason());
        return ResponseEntity.ok(personService.changeRole(id, verified));
    }

    /** Super-Admin-only — locks/unlocks an account without deleting it. */
    @PutMapping("/{id}/active")
    public ResponseEntity<PersonResponse> setActive(
            @PathVariable Long id, @Valid @RequestBody SetAccountActiveRequest request, Authentication authentication) {
        Long actorId = currentPersonResolver.resolveId(authentication);
        SetAccountActiveRequest verified = new SetAccountActiveRequest(request.active(), actorId, request.reason());
        return ResponseEntity.ok(personService.setActive(id, verified));
    }

    /** Super-Admin-only — every role change ever made, org-wide, newest first. */
    @GetMapping("/role-changes")
    public ResponseEntity<Page<RoleChangeResponse>> getRoleChangeActivity(Pageable pageable, Authentication authentication) {
        Long requesterId = currentPersonResolver.resolveId(authentication);
        return ResponseEntity.ok(personService.getRoleChangeActivity(requesterId, pageable));
    }

    /** Super-Admin-only — every account activation/deactivation ever made, org-wide,
     *  newest first. */
    @GetMapping("/account-status-changes")
    public ResponseEntity<Page<AccountStatusChangeResponse>> getAccountStatusChangeActivity(
            Pageable pageable, Authentication authentication) {
        Long requesterId = currentPersonResolver.resolveId(authentication);
        return ResponseEntity.ok(personService.getAccountStatusChangeActivity(requesterId, pageable));
    }

    /** Super-Admin-only — sends this person a password reset code (same flow as the
     *  self-service "forgot password"), for when they've lost access and can't request it
     *  themselves. Fails if they've never signed up (no password yet to reset). */
    @PostMapping("/{id}/send-password-reset")
    public ResponseEntity<Void> sendPasswordReset(
            @PathVariable Long id, @Valid @RequestBody SendPasswordResetRequest request, Authentication authentication) {
        Long actorId = currentPersonResolver.resolveId(authentication);
        SendPasswordResetRequest verified = new SendPasswordResetRequest(actorId, request.reason());
        personService.sendPasswordReset(id, verified);
        return ResponseEntity.ok().build();
    }

    // No PUT /{id}/team/{teamId} — a person can belong to multiple teams now, so "assign this
    // person to a team" is no longer a single-target operation. Use
    // POST /api/v1/teams/{teamId}/members instead (TeamController), which also carries the
    // mandatory reason and enforces who's allowed to do it.
}
