package com.throughline.taskmanagement.controller;

import com.throughline.taskmanagement.dto.request.CreateGuidanceNoteRequest;
import com.throughline.taskmanagement.dto.request.UpdateGuidanceNoteRequest;
import com.throughline.taskmanagement.dto.response.GuidanceNoteResponse;
import com.throughline.taskmanagement.security.CurrentPersonResolver;
import com.throughline.taskmanagement.service.IncidentGuidanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** createdById/changedById/actorId are always re-derived from the caller's actual login
 *  (CurrentPersonResolver), never trusted from the request body — same rule as every other
 *  controller in this app. */
@RestController
@RequestMapping("/api/v1/incidents/guidance-notes")
@RequiredArgsConstructor
public class IncidentGuidanceController {

    private final IncidentGuidanceService incidentGuidanceService;
    private final CurrentPersonResolver currentPersonResolver;

    @GetMapping
    public ResponseEntity<List<GuidanceNoteResponse>> getNotes() {
        return ResponseEntity.ok(incidentGuidanceService.getNotes());
    }

    @PostMapping
    public ResponseEntity<GuidanceNoteResponse> createNote(
            @Valid @RequestBody CreateGuidanceNoteRequest request, Authentication authentication) {
        Long actorId = currentPersonResolver.resolveId(authentication);
        CreateGuidanceNoteRequest verified = new CreateGuidanceNoteRequest(request.title(), request.body(), actorId);
        return new ResponseEntity<>(incidentGuidanceService.createNote(verified), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<GuidanceNoteResponse> updateNote(
            @PathVariable Long id, @Valid @RequestBody UpdateGuidanceNoteRequest request, Authentication authentication) {
        Long actorId = currentPersonResolver.resolveId(authentication);
        UpdateGuidanceNoteRequest verified = new UpdateGuidanceNoteRequest(request.title(), request.body(), actorId);
        return ResponseEntity.ok(incidentGuidanceService.updateNote(id, verified));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNote(@PathVariable Long id, Authentication authentication) {
        Long actorId = currentPersonResolver.resolveId(authentication);
        incidentGuidanceService.deleteNote(id, actorId);
        return ResponseEntity.noContent().build();
    }
}
