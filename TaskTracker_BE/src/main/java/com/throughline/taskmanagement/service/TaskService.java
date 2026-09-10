package com.throughline.taskmanagement.service;

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
import com.throughline.taskmanagement.dto.response.ReassignmentResponse;
import com.throughline.taskmanagement.dto.response.TaskActivityResponse;
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
    /** assignedPersonId is optional — a Director/Super Admin omits it to see everything; a
     *  Member's frontend always passes their own id, since "all tasks" isn't theirs to see. */
    Page<TaskListResponse> getAllTasks(TaskStatus status, Long assignedPersonId, Pageable pageable);
    TaskDetailResponse addProgressComment(Long taskId, AddCommentRequest request);
    /** A plain Q&A message, fully open — any authenticated person may post on any task,
     *  same as the progress log always has been. Never touches percentage/status.
     *  parentCommentId (in the request) threads it under a top-level comment, one level
     *  deep, same as Instagram. */
    TaskDetailResponse addDiscussionComment(Long taskId, AddDiscussionCommentRequest request);
    TaskDetailResponse reassignTask(Long taskId, ReassignTaskRequest request);
    Page<TaskTimelineResponse> getTaskProgressTimeline(Long taskId, Pageable pageable);
    Page<CommentResponse> getTaskComments(Long taskId, Pageable pageable);
    Page<ReassignmentResponse> getTaskReassignments(Long taskId, Pageable pageable);
    /** assignedPersonId is optional — same "Director/Super Admin sees everything, a Member
     *  is scoped to their own" rule as getAllTasks. */
    Page<TaskListResponse> searchTasks(String q, Long assignedPersonId, Pageable pageable);
    TaskDetailResponse updateTask(Long id, UpdateTaskRequest request);
    /** Director/Super-Admin-only, enforced here (not just by the frontend hiding the
     *  button). actorId is the caller's real, JWT-resolved identity. */
    void deleteTask(Long id, Long actorId);

    /** Every task/subtask creation and deletion, org-wide, newest first — Director or
     *  Super Admin only (broader than the role-change/account-status-change logs, which
     *  are Super-Admin-only). requesterId is the caller's real, JWT-resolved identity. */
    Page<TaskActivityResponse> getTaskActivity(Long requesterId, Pageable pageable);

    /** Requester must be this task's own accountable person (Team Leader/individual
     *  assignee/Department head Director) or a Director-or-above override (Executive-or-
     *  above for a Department task) — enforced here. Leaves the task's deadline unchanged
     *  until a decision is made. */
    TaskDetailResponse requestDeadlineExtension(Long taskId, RequestDeadlineExtensionRequest request);

    /** Decider must be this task's own setter (assignedBy) or the same override tier as
     *  requesting — enforced here. Approving moves the task's deadline; rejecting leaves
     *  it untouched. Fails if the request is already decided or belongs to a different task. */
    TaskDetailResponse decideDeadlineExtension(Long taskId, Long extensionRequestId, DecideDeadlineExtensionRequest request);

    /** Same authority as deciding a request — moves the deadline immediately, no approval
     *  round-trip, but still logs a self-approved TaskDeadlineExtensionRequest row so the
     *  audit trail has no gap. */
    TaskDetailResponse extendDeadlineDirectly(Long taskId, ExtendDeadlineRequest request);

    /** Every request/decision for this task's deadline, newest first — open read, same as
     *  getTaskReassignments. */
    Page<DeadlineExtensionResponse> getDeadlineHistory(Long taskId, Pageable pageable);

    /** Director-or-above only, enforced here. A manual, independently-editable toggle —
     *  not derived from severity, so any task can be pinned/unpinned regardless of its
     *  severity classification. */
    TaskDetailResponse setPinned(Long taskId, SetPinnedRequest request);
}
