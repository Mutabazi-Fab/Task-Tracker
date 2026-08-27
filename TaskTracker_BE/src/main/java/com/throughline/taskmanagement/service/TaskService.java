package com.throughline.taskmanagement.service;

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
import com.throughline.taskmanagement.enums.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TaskService {
    /** Creates a top-level task only — Director-only, always team-assigned. */
    TaskDetailResponse createTask(CreateTaskRequest request);
    /** Creates a subtask under an existing top-level task — the parent's Team Leader or a Director. */
    TaskDetailResponse createSubtask(Long parentTaskId, CreateSubtaskRequest request);
    TaskDetailResponse getTaskById(Long id);
    TaskDetailResponse getTaskByCode(String taskCode);
    Page<TaskListResponse> getAllTasks(TaskStatus status, Pageable pageable);
    TaskDetailResponse addProgressComment(Long taskId, AddCommentRequest request);
    TaskDetailResponse reassignTask(Long taskId, ReassignTaskRequest request);
    Page<TaskTimelineResponse> getTaskProgressTimeline(Long taskId, Pageable pageable);
    Page<CommentResponse> getTaskComments(Long taskId, Pageable pageable);
    Page<ReassignmentResponse> getTaskReassignments(Long taskId, Pageable pageable);
    Page<TaskListResponse> searchTasks(String q, Pageable pageable);
    TaskDetailResponse updateTask(Long id, UpdateTaskRequest request);
    void deleteTask(Long id);
}
