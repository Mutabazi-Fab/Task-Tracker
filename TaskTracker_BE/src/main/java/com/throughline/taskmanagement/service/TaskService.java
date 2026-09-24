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
import com.throughline.taskmanagement.dto.response.DocumentDownload;
import com.throughline.taskmanagement.dto.response.PendingExtensionRequestResponse;
import com.throughline.taskmanagement.dto.response.ReassignmentResponse;
import com.throughline.taskmanagement.dto.response.TaskActivityResponse;
import com.throughline.taskmanagement.dto.response.TaskDetailResponse;
import com.throughline.taskmanagement.dto.response.TaskListResponse;
import com.throughline.taskmanagement.dto.response.TaskTimelineResponse;
import com.throughline.taskmanagement.enums.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TaskService {
    /** Creates a top-level task only — Director-only, always team-assigned. */
    TaskDetailResponse createTask(CreateTaskRequest request);
    /** Creates a subtask under an existing top-level task — the parent's Team Leader or a Director. */
    TaskDetailResponse createSubtask(Long parentTaskId, CreateSubtaskRequest request);
    TaskDetailResponse getTaskById(Long id);
    TaskDetailResponse getTaskByCode(String taskCode);
    /** assignedPersonId is optional — a Member's frontend always passes their own id; a
     *  Director/Executive/Super Admin omits it. departmentId is the second, independent
     *  scope (a plain Director's own department), checked only when assignedPersonId is
     *  absent. */
    Page<TaskListResponse> getAllTasks(TaskStatus status, Long assignedPersonId, Long departmentId, Pageable pageable);
    TaskDetailResponse addProgressComment(Long taskId, AddCommentRequest request);
    /** A plain Q&A message, fully open — any authenticated person may post on any task, same as the
     *  progress log always has been. */
    TaskDetailResponse addDiscussionComment(Long taskId, AddDiscussionCommentRequest request);
    TaskDetailResponse reassignTask(Long taskId, ReassignTaskRequest request);
    Page<TaskTimelineResponse> getTaskProgressTimeline(Long taskId, Pageable pageable);
    Page<CommentResponse> getTaskComments(Long taskId, Pageable pageable);
    Page<ReassignmentResponse> getTaskReassignments(Long taskId, Pageable pageable);
    /** assignedPersonId/departmentId — same scoping rules, and the same precedence between
     *  them, as getAllTasks. */
    Page<TaskListResponse> searchTasks(String q, Long assignedPersonId, Long departmentId, Pageable pageable);
    TaskDetailResponse updateTask(Long id, UpdateTaskRequest request);
    /** Director/Super-Admin-only, enforced here (not just by the frontend hiding the
     *  button). actorId is the caller's real, JWT-resolved identity. */
    void deleteTask(Long id, Long actorId);

    /** Every task/subtask creation and deletion, org-wide, newest first — Director or
     *  Super Admin only (broader than the role-change/account-status-change logs, which
     *  are Super-Admin-only). requesterId is the caller's real, JWT-resolved identity. */
    Page<TaskActivityResponse> getTaskActivity(Long requesterId, Pageable pageable);

    /** Requester must be this task's own accountable person (Team Leader/individual assignee/Department
     *  head Director) or a Director-or-above override (Executive-or- above for a Department task) —
     *  enforced here. */
    TaskDetailResponse requestDeadlineExtension(Long taskId, RequestDeadlineExtensionRequest request);

    /** Decider must be this task's own setter (assignedBy) or the same override tier as requesting —
     *  enforced here. */
    TaskDetailResponse decideDeadlineExtension(Long taskId, Long extensionRequestId, DecideDeadlineExtensionRequest request);

    /** Only meaningful on a CEO-mandated chain (see TaskServiceImpl.isCeoMandated) — the Director a request
     *  lands on can reject it but can't approve it, so this sends it into the true approver's (CEO/Super
     *  Admin's) "Requests" inbox. */
    TaskDetailResponse forwardExtensionRequestToApprover(Long taskId, Long extensionRequestId, Long forwardedById);

    /** Same authority as deciding a request — moves the deadline immediately, no approval
     *  round-trip, but still logs a self-approved TaskDeadlineExtensionRequest row so the
     *  audit trail has no gap. */
    TaskDetailResponse extendDeadlineDirectly(Long taskId, ExtendDeadlineRequest request);

    /** Every request/decision for this task's deadline, newest first — open read, same as
     *  getTaskReassignments. */
    Page<DeadlineExtensionResponse> getDeadlineHistory(Long taskId, Pageable pageable);

    /** Every still-PENDING deadline-extension request org-wide where deciderId is the one who'd decide it
     *  (see TaskServiceImpl.resolveDeadlineDecider) — the "Requests" inbox. */
    List<PendingExtensionRequestResponse> getPendingExtensionRequests(Long deciderId);

    /** Director-or-above only, enforced here. */
    TaskDetailResponse setPinned(Long taskId, SetPinnedRequest request);

    /** Any authenticated person, same as addDiscussionComment — no restriction beyond existing, since
     *  task-detail viewing itself has none either (see getTaskById). */
    TaskDetailResponse addDocument(Long taskId, String fileName, String contentType, byte[] content, Long uploadedById);

    /** Open read, same as getTaskReassignments/getDeadlineHistory — whoever can see the task
     *  can download anything attached to it. Fails if the document doesn't belong to taskId. */
    DocumentDownload getDocumentContent(Long taskId, Long documentId);

    /** Only the person who uploaded it, or a Director/Executive/Super Admin — enforced here. */
    TaskDetailResponse deleteDocument(Long taskId, Long documentId, Long actorId);
}
