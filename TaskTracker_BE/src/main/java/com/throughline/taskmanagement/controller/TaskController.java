package com.throughline.taskmanagement.controller;

import com.throughline.taskmanagement.dto.request.AddCommentRequest;
import com.throughline.taskmanagement.dto.request.CreateTaskRequest;
import com.throughline.taskmanagement.dto.request.ReassignTaskRequest;
import com.throughline.taskmanagement.dto.request.UpdateTaskRequest;
import com.throughline.taskmanagement.dto.response.CommentResponse;
import com.throughline.taskmanagement.dto.response.ReassignmentResponse;
import com.throughline.taskmanagement.dto.response.TaskDetailResponse;
import com.throughline.taskmanagement.dto.response.TaskListResponse;
import com.throughline.taskmanagement.dto.response.TaskTimelineResponse;
import com.throughline.taskmanagement.enums.TaskStatus;
import com.throughline.taskmanagement.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    public ResponseEntity<TaskDetailResponse> createTask(@Valid @RequestBody CreateTaskRequest request) {
        return new ResponseEntity<>(taskService.createTask(request), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<Page<TaskListResponse>> getAllTasks(
            @RequestParam(required = false) TaskStatus status,
            Pageable pageable) {
        return ResponseEntity.ok(taskService.getAllTasks(status, pageable));
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
    public ResponseEntity<List<TaskListResponse>> searchTasks(@RequestParam String q) {
        return ResponseEntity.ok(taskService.searchTasks(q));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskDetailResponse> updateTask(
            @PathVariable Long id, 
            @Valid @RequestBody UpdateTaskRequest request) {
        return ResponseEntity.ok(taskService.updateTask(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<TaskDetailResponse> addProgressComment(
            @PathVariable Long id, 
            @Valid @RequestBody AddCommentRequest request) {
        return ResponseEntity.ok(taskService.addProgressComment(id, request));
    }

    @GetMapping("/{id}/comments")
    public ResponseEntity<List<CommentResponse>> getTaskComments(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.getTaskById(id).comments());
    }

    @PostMapping("/{id}/reassign")
    public ResponseEntity<TaskDetailResponse> reassignTask(
            @PathVariable Long id, 
            @Valid @RequestBody ReassignTaskRequest request) {
        return ResponseEntity.ok(taskService.reassignTask(id, request));
    }

    @GetMapping("/{id}/reassignments")
    public ResponseEntity<List<ReassignmentResponse>> getTaskReassignments(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.getTaskById(id).reassignments());
    }

    @GetMapping("/{id}/progress-timeline")
    public ResponseEntity<List<TaskTimelineResponse>> getTaskProgressTimeline(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.getTaskProgressTimeline(id));
    }
}
