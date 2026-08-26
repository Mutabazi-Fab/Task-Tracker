package com.throughline.taskmanagement.controller;

import com.throughline.taskmanagement.dto.request.CreatePersonRequest;
import com.throughline.taskmanagement.dto.response.PersonResponse;
import com.throughline.taskmanagement.dto.response.PersonStatisticsResponse;
import com.throughline.taskmanagement.dto.response.PersonTaskHistoryResponse;
import com.throughline.taskmanagement.service.PersonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<List<PersonResponse>> getAllPeople() {
        return ResponseEntity.ok(personService.getAllPeople());
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
    public ResponseEntity<List<PersonTaskHistoryResponse>> getPersonTaskHistory(@PathVariable Long id) {
        return ResponseEntity.ok(personService.getPersonTaskHistory(id));
    }

    @PutMapping("/{id}/team/{teamId}")
    public ResponseEntity<PersonResponse> assignToTeam(
            @PathVariable Long id, 
            @PathVariable Long teamId) {
        return ResponseEntity.ok(personService.assignToTeam(id, teamId));
    }
}
