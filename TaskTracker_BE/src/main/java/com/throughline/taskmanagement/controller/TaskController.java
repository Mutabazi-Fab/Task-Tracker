package com.throughline.taskmanagement.controller;

import com.throughline.taskmanagement.dto.request.AddCommentRequest;
import com.throughline.taskmanagement.dto.request.CreateSubtaskRequest;
import com.throughline.taskmanagement.dto.request.CreateTaskRequest;
import com.throughline.taskmanagement.dto.request.ReassignTaskRequest;
import com.throughline.taskmanagement.dto.request.UpdateTaskRequest;
import com.throughline.taskmanagement.dto.response.CommentResponse;
import com.throughline.taskmanagement.dto.response.ReassignmentResponse;
import com.throughline.taskmanagement.dto.response.TaskDetailResponse;
import com.throughline.taskmanagement.dto.response.TaskListResponse;
import com.throughline.taskmanagement.dto.response.TaskTimelineResponse;
import com.throughline.taskmanagement.enums.Role;
import com.throughline.taskmanagement.enums.TaskStatus;
import com.throughline.taskmanagement.model.Person;
import com.throughline.taskmanagement.security.CurrentPersonResolver;
import com.throughline.taskmanagement.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Every "who's doing this" field (createdById/authorId/reassignedById) is re-derived from
 * the caller's actual login (CurrentPersonResolver), not trusted from the request — see
 * TeamController for the same pattern. assignedPersonId on the two read endpoints is
 * likewise forced to the caller's own id unless they're a Director/Super Admin, who see
 * everything — a Member can no longer get "all tasks" just by leaving that param off. Once
 * scoped to a person, "their tasks" means what TaskRepository.findVisibleToPerson defines:
 * tasks assigned to them directly PLUS top-level tasks assigned to any team they belong to.
 */
@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final CurrentPersonResolver currentPersonResolver;

    @PostMapping
    public ResponseEntity<TaskDetailResponse> createTask(@Valid @RequestBody CreateTaskRequest request, Authentication authentication) {
        Long actorId = currentPersonResolver.resolveId(authentication);
        CreateTaskRequest verified = new CreateTaskRequest(
                request.title(), request.description(), actorId, request.assignedTeamId(),
                request.dateAssigned(), request.openingNote());
        return new ResponseEntity<>(taskService.createTask(verified), HttpStatus.CREATED);
    }

    @PostMapping("/{parentTaskId}/subtasks")
    public ResponseEntity<TaskDetailResponse> createSubtask(
            @PathVariable Long parentTaskId,
            @Valid @RequestBody CreateSubtaskRequest request,
            Authentication authentication) {
        Long actorId = currentPersonResolver.resolveId(authentication);
        CreateSubtaskRequest verified = new CreateSubtaskRequest(
                request.title(), request.description(), actorId, request.assignedPersonId(),
                request.dateAssigned(), request.openingNote());
        return new ResponseEntity<>(taskService.createSubtask(parentTaskId, verified), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<Page<TaskListResponse>> getAllTasks(
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) Long assignedPersonId,
            Pageable pageable,
            Authentication authentication) {
        Long scopedPersonId = scopeToSelfUnlessDirector(assignedPersonId, authentication);
        return ResponseEntity.ok(taskService.getAllTasks(status, scopedPersonId, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskDetailResponse> getTaskById(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.getTaskById(id));
    }

    @GetMapping("/code/{taskCode}")
    public ResponseEntity<TaskDetailResponse> getTaskByCode(@PathVariable String taskCode) {
        return ResponseEntity.ok(taskService.getTaskByCode(taskCode));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<TaskListResponse>> searchTasks(
            @RequestParam String q,
            @RequestParam(required = false) Long assignedPersonId,
            Pageable pageable,
            Authentication authentication) {
        Long scopedPersonId = scopeToSelfUnlessDirector(assignedPersonId, authentication);
        return ResponseEntity.ok(taskService.searchTasks(q, scopedPersonId, pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskDetailResponse> updateTask(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTaskRequest request) {
        return ResponseEntity.ok(taskService.updateTask(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id, Authentication authentication) {
        Long actorId = currentPersonResolver.resolveId(authentication);
        taskService.deleteTask(id, actorId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<TaskDetailResponse> addProgressComment(
            @PathVariable Long id,
            @Valid @RequestBody AddCommentRequest request,
            Authentication authentication) {
        Long actorId = currentPersonResolver.resolveId(authentication);
        AddCommentRequest verified = new AddCommentRequest(actorId, request.percentageAtComment(), request.body());
        return ResponseEntity.ok(taskService.addProgressComment(id, verified));
    }

    @GetMapping("/{id}/comments")
    public ResponseEntity<Page<CommentResponse>> getTaskComments(@PathVariable Long id, Pageable pageable) {
        return ResponseEntity.ok(taskService.getTaskComments(id, pageable));
    }

    @PostMapping("/{id}/reassign")
    public ResponseEntity<TaskDetailResponse> reassignTask(
            @PathVariable Long id,
            @Valid @RequestBody ReassignTaskRequest request,
            Authentication authentication) {
        Long actorId = currentPersonResolver.resolveId(authentication);
        ReassignTaskRequest verified = new ReassignTaskRequest(request.newTeamId(), request.newPersonId(), actorId, request.reason());
        return ResponseEntity.ok(taskService.reassignTask(id, verified));
    }

    @GetMapping("/{id}/reassignments")
    public ResponseEntity<Page<ReassignmentResponse>> getTaskReassignments(@PathVariable Long id, Pageable pageable) {
        return ResponseEntity.ok(taskService.getTaskReassignments(id, pageable));
    }

    @GetMapping("/{id}/progress-timeline")
    public ResponseEntity<Page<TaskTimelineResponse>> getTaskProgressTimeline(@PathVariable Long id, Pageable pageable) {
        return ResponseEntity.ok(taskService.getTaskProgressTimeline(id, pageable));
    }

    /** A Director/Super Admin may pass any assignedPersonId (or none, to see everything);
     *  anyone else always gets scoped to themself, regardless of what was asked for. */
    private Long scopeToSelfUnlessDirector(Long requestedPersonId, Authentication authentication) {
        Person viewer = currentPersonResolver.resolve(authentication);
        if (Role.isAtLeastDirector(viewer.getRole())) {
            return requestedPersonId;
        }
        return viewer.getId();
    }
}
