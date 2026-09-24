package com.throughline.taskmanagement.controller;

import com.throughline.taskmanagement.dto.request.AddTaskSourceEntryRequest;
import com.throughline.taskmanagement.dto.response.TaskSourceEntryResponse;
import com.throughline.taskmanagement.security.CurrentPersonResolver;
import com.throughline.taskmanagement.service.TaskSourceEntryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Reusable "Source Detail" suggestions per TaskSource category (e.g. */
@RestController
@RequestMapping("/api/v1/task-source-entries")
@RequiredArgsConstructor
public class TaskSourceEntryController {

    private final TaskSourceEntryService taskSourceEntryService;
    private final CurrentPersonResolver currentPersonResolver;

    @GetMapping
    public ResponseEntity<List<TaskSourceEntryResponse>> getEntries(@RequestParam String source) {
        return ResponseEntity.ok(taskSourceEntryService.getEntries(source));
    }

    @PostMapping
    public ResponseEntity<TaskSourceEntryResponse> addEntry(
            @Valid @RequestBody AddTaskSourceEntryRequest request, Authentication authentication) {
        Long actorId = currentPersonResolver.resolveId(authentication);
        AddTaskSourceEntryRequest verified = new AddTaskSourceEntryRequest(request.source(), request.label(), actorId);
        return new ResponseEntity<>(taskSourceEntryService.addEntry(verified), HttpStatus.CREATED);
    }
}
