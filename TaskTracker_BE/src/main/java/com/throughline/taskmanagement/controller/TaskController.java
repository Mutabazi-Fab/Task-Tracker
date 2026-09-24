package com.throughline.taskmanagement.controller;

import com.throughline.taskmanagement.access.TaskAccessPolicy;
import com.throughline.taskmanagement.dto.request.AddCommentRequest;
import com.throughline.taskmanagement.dto.request.AddDiscussionCommentRequest;
import com.throughline.taskmanagement.dto.request.CreateSubtaskRequest;
import com.throughline.taskmanagement.dto.request.CreateTaskRequest;
import com.throughline.taskmanagement.dto.request.DecideDeadlineExtensionRequest;
import com.throughline.taskmanagement.dto.request.ExtendDeadlineRequest;
import com.throughline.taskmanagement.dto.request.ReassignTaskRequest;
import com.throughline.taskmanagement.dto.request.RequestDeadlineExtensionRequest;
import com.throughline.taskmanagement.dto.request.SetPinnedRequest;
import com.throughline.taskmanagement.dto.request.UpdateTaskRequest;
import com.throughline.taskmanagement.dto.response.CommentResponse;
import com.throughline.taskmanagement.dto.response.DeadlineExtensionResponse;
import com.throughline.taskmanagement.dto.response.DocumentDownload;
import com.throughline.taskmanagement.dto.response.PendingExtensionRequestResponse;
import com.throughline.taskmanagement.dto.response.ReassignmentResponse;
import com.throughline.taskmanagement.dto.response.TaskActivityResponse;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * Every "who's doing this" field (createdById/authorId/reassignedById) is re-derived from
 * the caller's actual login (CurrentPersonResolver), never trusted from the request.
 * assignedPersonId on the read endpoints is likewise forced to the caller's own id unless
 * they're a Director/Super Admin — see TaskRepository.findVisibleToPerson for what "their
 * tasks" actually resolves to.
 */
@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final CurrentPersonResolver currentPersonResolver;
    private final TaskAccessPolicy taskAccessPolicy;

    /** Every endpoint below that opens or acts on ONE task first checks the caller may see it — see TaskAccessPolicy. */
    private void requireVisible(Long taskId, Authentication authentication) {
        taskAccessPolicy.requireCanView(taskId, currentPersonResolver.resolveId(authentication));
    }

    @PostMapping
    public ResponseEntity<TaskDetailResponse> createTask(@Valid @RequestBody CreateTaskRequest request, Authentication authentication) {
        Long actorId = currentPersonResolver.resolveId(authentication);
        CreateTaskRequest verified = new CreateTaskRequest(
                request.title(), request.description(), actorId, request.assignedTeamId(),
                request.assignedPersonId(), request.assignedDepartmentId(), request.dateAssigned(),
                request.deadline(), request.source(), request.sourceLabel(), request.severity(), request.openingNote());
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
                request.assignedTeamId(), request.dateAssigned(), request.deadline(),
                request.source(), request.sourceLabel(), request.severity(), request.openingNote());
        return new ResponseEntity<>(taskService.createSubtask(parentTaskId, verified), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<Page<TaskListResponse>> getAllTasks(
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) Long assignedPersonId,
            Pageable pageable,
            Authentication authentication) {
        Person viewer = currentPersonResolver.resolve(authentication);
        Long scopedPersonId = scopeToSelfUnlessDirector(assignedPersonId, viewer);
        Long departmentId = departmentScopeForViewer(viewer);
        return ResponseEntity.ok(taskService.getAllTasks(status, scopedPersonId, departmentId, viewer.getId(), pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskDetailResponse> getTaskById(@PathVariable Long id, Authentication authentication) {
        requireVisible(id, authentication);
        return ResponseEntity.ok(taskService.getTaskById(id));
    }

    @GetMapping("/code/{taskCode}")
    public ResponseEntity<TaskDetailResponse> getTaskByCode(@PathVariable String taskCode, Authentication authentication) {
        TaskDetailResponse detail = taskService.getTaskByCode(taskCode);
        requireVisible(detail.id(), authentication);
        return ResponseEntity.ok(detail);
    }

    @GetMapping("/search")
    public ResponseEntity<Page<TaskListResponse>> searchTasks(
            @RequestParam String q,
            @RequestParam(required = false) Long assignedPersonId,
            Pageable pageable,
            Authentication authentication) {
        Person viewer = currentPersonResolver.resolve(authentication);
        Long scopedPersonId = scopeToSelfUnlessDirector(assignedPersonId, viewer);
        Long departmentId = departmentScopeForViewer(viewer);
        return ResponseEntity.ok(taskService.searchTasks(q, scopedPersonId, departmentId, viewer.getId(), pageable));
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

    /** Director or Super Admin only — every task/subtask created or deleted, org-wide. A
     *  static path, so it's matched ahead of GET /{id} the same way GET /search already is. */
    @GetMapping("/activity")
    public ResponseEntity<Page<TaskActivityResponse>> getTaskActivity(Pageable pageable, Authentication authentication) {
        Long requesterId = currentPersonResolver.resolveId(authentication);
        return ResponseEntity.ok(taskService.getTaskActivity(requesterId, pageable));
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<TaskDetailResponse> addProgressComment(
            @PathVariable Long id,
            @Valid @RequestBody AddCommentRequest request,
            Authentication authentication) {
        Long actorId = currentPersonResolver.resolveId(authentication);
        requireVisible(id, authentication);
        AddCommentRequest verified = new AddCommentRequest(actorId, request.percentageAtComment(), request.body());
        return ResponseEntity.ok(taskService.addProgressComment(id, verified));
    }

    @GetMapping("/{id}/comments")
    public ResponseEntity<Page<CommentResponse>> getTaskComments(@PathVariable Long id, Pageable pageable, Authentication authentication) {
        requireVisible(id, authentication);
        return ResponseEntity.ok(taskService.getTaskComments(id, pageable));
    }

    /** Fully open — any authenticated person may post on any task, same as the progress
     *  log always has been (see TaskService.addDiscussionComment). */
    @PostMapping("/{id}/discussion-comments")
    public ResponseEntity<TaskDetailResponse> addDiscussionComment(
            @PathVariable Long id,
            @Valid @RequestBody AddDiscussionCommentRequest request,
            Authentication authentication) {
        Long actorId = currentPersonResolver.resolveId(authentication);
        requireVisible(id, authentication);
        AddDiscussionCommentRequest verified = new AddDiscussionCommentRequest(actorId, request.body(), request.parentCommentId());
        return new ResponseEntity<>(taskService.addDiscussionComment(id, verified), HttpStatus.CREATED);
    }

    @PostMapping("/{id}/reassign")
    public ResponseEntity<TaskDetailResponse> reassignTask(
            @PathVariable Long id,
            @Valid @RequestBody ReassignTaskRequest request,
            Authentication authentication) {
        Long actorId = currentPersonResolver.resolveId(authentication);
        ReassignTaskRequest verified = new ReassignTaskRequest(
                request.newTeamId(), request.newPersonId(), request.newDepartmentId(), actorId, request.reason());
        return ResponseEntity.ok(taskService.reassignTask(id, verified));
    }

    @GetMapping("/{id}/reassignments")
    public ResponseEntity<Page<ReassignmentResponse>> getTaskReassignments(@PathVariable Long id, Pageable pageable, Authentication authentication) {
        requireVisible(id, authentication);
        return ResponseEntity.ok(taskService.getTaskReassignments(id, pageable));
    }

    @GetMapping("/{id}/progress-timeline")
    public ResponseEntity<Page<TaskTimelineResponse>> getTaskProgressTimeline(@PathVariable Long id, Pageable pageable, Authentication authentication) {
        requireVisible(id, authentication);
        return ResponseEntity.ok(taskService.getTaskProgressTimeline(id, pageable));
    }

    @PostMapping("/{id}/deadline-extensions")
    public ResponseEntity<TaskDetailResponse> requestDeadlineExtension(
            @PathVariable Long id,
            @Valid @RequestBody RequestDeadlineExtensionRequest request,
            Authentication authentication) {
        Long actorId = currentPersonResolver.resolveId(authentication);
        RequestDeadlineExtensionRequest verified =
                new RequestDeadlineExtensionRequest(request.requestedDeadline(), request.justification(), actorId);
        return new ResponseEntity<>(taskService.requestDeadlineExtension(id, verified), HttpStatus.CREATED);
    }

    @PutMapping("/{id}/deadline-extensions/{extensionId}")
    public ResponseEntity<TaskDetailResponse> decideDeadlineExtension(
            @PathVariable Long id,
            @PathVariable Long extensionId,
            @Valid @RequestBody DecideDeadlineExtensionRequest request,
            Authentication authentication) {
        Long actorId = currentPersonResolver.resolveId(authentication);
        DecideDeadlineExtensionRequest verified =
                new DecideDeadlineExtensionRequest(request.approve(), request.decisionNote(), actorId);
        return ResponseEntity.ok(taskService.decideDeadlineExtension(id, extensionId, verified));
    }

    /** Only meaningful on a CEO-mandated chain — sends a request into the true approver's (the CEO/Super
     *  Admin's) own "Requests" inbox; see TaskService. forwardExtensionRequestToApprover. */
    @PutMapping("/{id}/deadline-extensions/{extensionId}/forward")
    public ResponseEntity<TaskDetailResponse> forwardExtensionRequestToApprover(
            @PathVariable Long id,
            @PathVariable Long extensionId,
            Authentication authentication) {
        Long actorId = currentPersonResolver.resolveId(authentication);
        return ResponseEntity.ok(taskService.forwardExtensionRequestToApprover(id, extensionId, actorId));
    }

    @PutMapping("/{id}/deadline")
    public ResponseEntity<TaskDetailResponse> extendDeadlineDirectly(
            @PathVariable Long id,
            @Valid @RequestBody ExtendDeadlineRequest request,
            Authentication authentication) {
        Long actorId = currentPersonResolver.resolveId(authentication);
        ExtendDeadlineRequest verified = new ExtendDeadlineRequest(request.newDeadline(), request.reason(), actorId);
        return ResponseEntity.ok(taskService.extendDeadlineDirectly(id, verified));
    }

    @GetMapping("/{id}/deadline-extensions")
    public ResponseEntity<Page<DeadlineExtensionResponse>> getDeadlineHistory(@PathVariable Long id, Pageable pageable, Authentication authentication) {
        requireVisible(id, authentication);
        return ResponseEntity.ok(taskService.getDeadlineHistory(id, pageable));
    }

    /** The "Requests" inbox — every deadline-extension request still waiting on the
     *  caller's own decision, across every task, not just one task's own history panel.
     *  deciderId is the caller's real, JWT-resolved identity, same as every other
     *  "who's asking" field in this controller. */
    @GetMapping("/deadline-extensions/pending")
    public ResponseEntity<List<PendingExtensionRequestResponse>> getPendingExtensionRequests(Authentication authentication) {
        Long deciderId = currentPersonResolver.resolveId(authentication);
        return ResponseEntity.ok(taskService.getPendingExtensionRequests(deciderId));
    }

    @PutMapping("/{id}/pin")
    public ResponseEntity<TaskDetailResponse> setPinned(
            @PathVariable Long id,
            @Valid @RequestBody SetPinnedRequest request,
            Authentication authentication) {
        Long actorId = currentPersonResolver.resolveId(authentication);
        SetPinnedRequest verified = new SetPinnedRequest(request.pinned(), actorId);
        return ResponseEntity.ok(taskService.setPinned(id, verified));
    }

    /** Open to any authenticated person, same as a discussion comment — see TaskService.
     *  addDocument. uploadedById is always the caller's own real identity. */
    @PostMapping(value = "/{id}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TaskDetailResponse> addDocument(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) throws IOException {
        Long actorId = currentPersonResolver.resolveId(authentication);
        requireVisible(id, authentication);
        return ResponseEntity.ok(taskService.addDocument(
                id, file.getOriginalFilename(), file.getContentType(), file.getBytes(), actorId));
    }

    /** Open read, same as getTaskReassignments/getDeadlineHistory — whoever can see the task
     *  can download anything attached to it. */
    @GetMapping("/{id}/documents/{documentId}/download")
    public ResponseEntity<byte[]> downloadDocument(@PathVariable Long id, @PathVariable Long documentId, Authentication authentication) {
        requireVisible(id, authentication);
        DocumentDownload document = taskService.getDocumentContent(id, documentId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(document.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + document.fileName() + "\"")
                .body(document.content());
    }

    @DeleteMapping("/{id}/documents/{documentId}")
    public ResponseEntity<TaskDetailResponse> deleteDocument(
            @PathVariable Long id,
            @PathVariable Long documentId,
            Authentication authentication) {
        Long actorId = currentPersonResolver.resolveId(authentication);
        requireVisible(id, authentication);
        return ResponseEntity.ok(taskService.deleteDocument(id, documentId, actorId));
    }

    /** A Director/Executive/Super Admin may pass any assignedPersonId (or none, to skip
     *  person-scoping entirely — a plain Director still gets department-scoped instead, see
     *  departmentScopeForViewer); anyone else always gets scoped to themself, regardless of
     *  what was asked for. */
    private Long scopeToSelfUnlessDirector(Long requestedPersonId, Person viewer) {
        if (Role.isAtLeastDirector(viewer.getRole())) {
            return requestedPersonId;
        }
        return viewer.getId();
    }

    /** A plain Director (role DIRECTOR exactly) only sees tasks in their own department, once
     *  assignedPersonId comes back null from scopeToSelfUnlessDirector above. */
    private Long departmentScopeForViewer(Person viewer) {
        if (viewer.getRole() != Role.DIRECTOR) {
            return null;
        }
        return viewer.getDepartment() != null ? viewer.getDepartment().getId() : null;
    }
}
