package com.throughline.taskmanagement.controller;

import com.throughline.taskmanagement.dto.request.ChangeRoleRequest;
import com.throughline.taskmanagement.dto.request.CreatePersonRequest;
import com.throughline.taskmanagement.dto.request.SetAccountActiveRequest;
import com.throughline.taskmanagement.dto.response.PersonResponse;
import com.throughline.taskmanagement.dto.response.PersonStatisticsResponse;
import com.throughline.taskmanagement.dto.response.PersonTaskHistoryResponse;
import com.throughline.taskmanagement.dto.response.RoleChangeResponse;
import com.throughline.taskmanagement.service.PersonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/people")
@RequiredArgsConstructor
public class PersonController {

    private final PersonService personService;

    @PostMapping
    public ResponseEntity<PersonResponse> createPerson(@Valid @RequestBody CreatePersonRequest request) {
        return new ResponseEntity<>(personService.createPerson(request), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<Page<PersonResponse>> getAllPeople(Pageable pageable) {
        return ResponseEntity.ok(personService.getAllPeople(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PersonResponse> getPersonById(@PathVariable Long id) {
        return ResponseEntity.ok(personService.getPersonById(id));
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
    public ResponseEntity<PersonStatisticsResponse> getPersonStatistics(@PathVariable Long id) {
        return ResponseEntity.ok(personService.getPersonStatistics(id));
    }

    @GetMapping("/{id}/tasks")
    public ResponseEntity<Page<PersonTaskHistoryResponse>> getPersonTaskHistory(@PathVariable Long id, Pageable pageable) {
        return ResponseEntity.ok(personService.getPersonTaskHistory(id, pageable));
    }

    /** Super-Admin-only — promotes/demotes between Member, Director, and Super Admin. */
    @PutMapping("/{id}/role")
    public ResponseEntity<PersonResponse> changeRole(@PathVariable Long id, @Valid @RequestBody ChangeRoleRequest request) {
        return ResponseEntity.ok(personService.changeRole(id, request));
    }

    /** Super-Admin-only — locks/unlocks an account without deleting it. */
    @PutMapping("/{id}/active")
    public ResponseEntity<PersonResponse> setActive(@PathVariable Long id, @Valid @RequestBody SetAccountActiveRequest request) {
        return ResponseEntity.ok(personService.setActive(id, request));
    }

    /** Super-Admin-only — every role change ever made, org-wide, newest first. */
    @GetMapping("/role-changes")
    public ResponseEntity<Page<RoleChangeResponse>> getRoleChangeActivity(
            @RequestParam Long requesterId, Pageable pageable) {
        return ResponseEntity.ok(personService.getRoleChangeActivity(requesterId, pageable));
    }

    // No PUT /{id}/team/{teamId} — a person can belong to multiple teams now, so "assign this
    // person to a team" is no longer a single-target operation. Use
    // POST /api/v1/teams/{teamId}/members instead (TeamController), which also carries the
    // mandatory reason and enforces who's allowed to do it.
}
