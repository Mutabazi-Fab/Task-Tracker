package com.throughline.taskmanagement.service.impl;

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
import com.throughline.taskmanagement.enums.AssigneeType;
import com.throughline.taskmanagement.enums.CommentType;
import com.throughline.taskmanagement.enums.CreatedByRole;
import com.throughline.taskmanagement.enums.ExtensionRequestStatus;
import com.throughline.taskmanagement.enums.Role;
import com.throughline.taskmanagement.enums.TaskActivityAction;
import com.throughline.taskmanagement.enums.TaskSeverity;
import com.throughline.taskmanagement.enums.TaskSource;
import com.throughline.taskmanagement.enums.TaskStatus;
import com.throughline.taskmanagement.exception.ForbiddenActionException;
import com.throughline.taskmanagement.exception.InvalidAssignmentException;
import com.throughline.taskmanagement.exception.InvalidProgressException;
import com.throughline.taskmanagement.exception.ResourceNotFoundException;
import com.throughline.taskmanagement.mapper.TaskMapper;
import com.throughline.taskmanagement.model.Department;
import com.throughline.taskmanagement.model.Person;
import com.throughline.taskmanagement.model.Task;
import com.throughline.taskmanagement.model.TaskActivity;
import com.throughline.taskmanagement.model.TaskComment;
import com.throughline.taskmanagement.model.TaskDeadlineExtensionRequest;
import com.throughline.taskmanagement.model.TaskDocument;
import com.throughline.taskmanagement.model.TaskReassignment;
import com.throughline.taskmanagement.model.Team;
import com.throughline.taskmanagement.model.TeamMember;
import com.throughline.taskmanagement.repository.DepartmentRepository;
import com.throughline.taskmanagement.repository.PersonRepository;
import com.throughline.taskmanagement.repository.TaskActivityRepository;
import com.throughline.taskmanagement.repository.TaskCommentRepository;
import com.throughline.taskmanagement.repository.TaskDeadlineExtensionRequestRepository;
import com.throughline.taskmanagement.repository.TaskDocumentRepository;
import com.throughline.taskmanagement.repository.TaskReassignmentRepository;
import com.throughline.taskmanagement.repository.TaskRepository;
import com.throughline.taskmanagement.repository.TeamMemberRepository;
import com.throughline.taskmanagement.repository.TeamRepository;
import com.throughline.taskmanagement.service.NotificationService;
import com.throughline.taskmanagement.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import java.util.List;
import java.util.Set;

@Service
@Transactional
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final PersonRepository personRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final DepartmentRepository departmentRepository;
    private final TaskCommentRepository taskCommentRepository;
    private final TaskReassignmentRepository taskReassignmentRepository;
    private final TaskDeadlineExtensionRequestRepository taskDeadlineExtensionRequestRepository;
    private final TaskDocumentRepository taskDocumentRepository;
    private final TaskMapper taskMapper;
    private final NotificationService notificationService;
    private final TaskActivityRepository taskActivityRepository;

    @Override
    public TaskDetailResponse createTask(CreateTaskRequest request) {
        Person createdBy = personRepository.findById(request.createdById())
                .orElseThrow(() -> new ResourceNotFoundException("createdById not found"));
        if (!Role.isAtLeastDirector(createdBy.getRole())) {
            throw new ForbiddenActionException("Only a Director can create a top-level task.");
        }

        boolean hasTeam = request.assignedTeamId() != null;
        boolean hasPerson = request.assignedPersonId() != null;
        boolean hasDepartment = request.assignedDepartmentId() != null;
        if ((hasTeam ? 1 : 0) + (hasPerson ? 1 : 0) + (hasDepartment ? 1 : 0) != 1) {
            throw new InvalidAssignmentException("Provide exactly one of assignedTeamId, assignedPersonId, or assignedDepartmentId.");
        }
        if (hasDepartment && !Role.isAtLeastExecutive(createdBy.getRole())) {
            throw new ForbiddenActionException("Only an Executive or Super Admin can assign a task to a whole Department.");
        }
        requireReasonableDate(request.dateAssigned());
        requireDeadlineNotBeforeAssignment(request.dateAssigned(), request.deadline());

        Task task = new Task();
        task.setTaskCode(nextTaskCode());
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setDateAssigned(request.dateAssigned());
        task.setDeadline(request.deadline());
        task.setAssignedBy(createdBy);
        task.setParentTask(null);
        task.setDepth(0);
        task.setCreatedByRole(CreatedByRole.DIRECTOR);
        task.setProgressPercentage(0);
        task.setStatus(TaskStatus.PENDING);
        applySourceAndSeverity(task, request.source(), request.sourceLabel(), request.severity(), createdBy);

        if (hasTeam) {
            Team assignedTeam = teamRepository.findById(request.assignedTeamId())
                    .orElseThrow(() -> new ResourceNotFoundException("assignedTeamId not found"));
            requireCanAssignWithinDepartment(createdBy, assignedTeam.getDepartment());
            task.setAssigneeType(AssigneeType.TEAM);
            task.setAssignedTeam(assignedTeam);
        } else if (hasPerson) {
            Person assignedPerson = personRepository.findById(request.assignedPersonId())
                    .orElseThrow(() -> new ResourceNotFoundException("assignedPersonId not found"));
            requireCanAssignWithinDepartment(createdBy, assignedPerson.getDepartment());
            task.setAssigneeType(AssigneeType.INDIVIDUAL);
            task.setAssignedPerson(assignedPerson);
        } else {
            Department assignedDepartment = departmentRepository.findById(request.assignedDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("assignedDepartmentId not found"));
            task.setAssigneeType(AssigneeType.DEPARTMENT);
            task.setAssignedDepartment(assignedDepartment);
        }

        Task savedTask = taskRepository.save(task);
        addOpeningComment(savedTask, createdBy, request.openingNote());
        notificationService.notifyTaskAssigned(savedTask, createdBy);
        recordActivity(savedTask, TaskActivityAction.CREATED, createdBy);

        return taskMapper.toDetailResponse(savedTask);
    }

    @Override
    public TaskDetailResponse createSubtask(Long parentTaskId, CreateSubtaskRequest request) {
        Task parent = taskRepository.findWithDetailsById(parentTaskId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent task not found"));

        if (parent.getDepth() >= 2) {
            throw new InvalidAssignmentException("Cannot create a subtask under a task that's already at maximum depth.");
        }
        requireReasonableDate(request.dateAssigned());
        requireDeadlineNotBeforeAssignment(request.dateAssigned(), request.deadline());

        Person createdBy = personRepository.findById(request.createdById())
                .orElseThrow(() -> new ResourceNotFoundException("createdById not found"));

        // A DEPARTMENT-typed parent's child is the new depth-1 "implementation task"
        // (team- or individual-assigned, org-wide); every other parent shape uses the
        // ordinary leaf-subtask case (always INDIVIDUAL, always on the parent's own team).
        if (parent.getAssigneeType() == AssigneeType.DEPARTMENT) {
            return createImplementationTask(parent, request, createdBy);
        }
        return createLeafSubtask(parent, request, createdBy);
    }

    /** The classic case, unchanged in shape from before Phase B: parent must be TEAM-
     *  assigned (a plain top-level task, or a depth-1 Department implementation task),
     *  and the new subtask is always INDIVIDUAL, always a member of that same team. */
    private TaskDetailResponse createLeafSubtask(Task parent, CreateSubtaskRequest request, Person createdBy) {
        if (parent.getAssignedTeam() == null) {
            throw new InvalidAssignmentException("Parent task has no assigned team.");
        }
        if (request.assignedPersonId() == null) {
            throw new InvalidAssignmentException("assignedPersonId is required.");
        }
        if (request.assignedTeamId() != null) {
            throw new InvalidAssignmentException("A leaf subtask is always assigned to a person, not a team.");
        }
        Long teamId = parent.getAssignedTeam().getId();

        // isDirectorRole only feeds the CreatedByRole audit label below — the actual
        // authorization check is isHeadOfDepartment/isThisTeamsLeader further down.
        boolean isDirectorRole = Role.isAtLeastDirector(createdBy.getRole());
        boolean headsThisDepartment = isHeadOfDepartment(createdBy, parent.getAssignedTeam().getDepartment());
        boolean isThisTeamsLeader = teamMemberRepository.findByTeamIdAndPersonId(teamId, createdBy.getId())
                .map(TeamMember::isLeader)
                .orElse(false);
        if (!headsThisDepartment && !isThisTeamsLeader) {
            throw new ForbiddenActionException(
                    "Only the parent task's Team Leader, or a Director who heads its department (or an Executive/Super Admin), can create a subtask.");
        }

        Person assignedPerson = personRepository.findById(request.assignedPersonId())
                .orElseThrow(() -> new ResourceNotFoundException("assignedPersonId not found"));
        if (!teamMemberRepository.existsByTeamIdAndPersonId(teamId, request.assignedPersonId())) {
            throw new InvalidAssignmentException("Subtask assignee must be a member of the parent task's team.");
        }

        Task subtask = new Task();
        subtask.setTaskCode(nextTaskCode());
        subtask.setTitle(request.title());
        subtask.setDescription(request.description());
        subtask.setDateAssigned(request.dateAssigned());
        subtask.setDeadline(request.deadline());
        subtask.setAssignedBy(createdBy);
        subtask.setAssigneeType(AssigneeType.INDIVIDUAL);
        subtask.setAssignedPerson(assignedPerson);
        subtask.setParentTask(parent);
        subtask.setDepth(parent.getDepth() + 1);
        subtask.setCreatedByRole(isDirectorRole ? CreatedByRole.DIRECTOR : CreatedByRole.TEAM_LEADER);
        subtask.setProgressPercentage(0);
        subtask.setStatus(TaskStatus.PENDING);
        applySourceAndSeverity(subtask, request.source(), request.sourceLabel(), request.severity(), createdBy);

        Task savedSubtask = taskRepository.save(subtask);
        addOpeningComment(savedSubtask, createdBy, request.openingNote());

        parent.getSubtasks().add(savedSubtask);
        recalculateParentRollup(parent);
        notificationService.notifySubtaskAssigned(savedSubtask, createdBy);
        recordActivity(savedSubtask, TaskActivityAction.CREATED, createdBy);

        return taskMapper.toDetailResponse(savedSubtask);
    }

    /** The depth-1 case: parent is a Department task. Its "implementation task" is team-
     *  or individual-assigned, like a fresh top-level task (see createTask), with no team-
     *  membership restriction. Restricted to that Department's own head Director (or an
     *  Executive/Super Admin override). */
    private TaskDetailResponse createImplementationTask(Task parent, CreateSubtaskRequest request, Person createdBy) {
        Department department = parent.getAssignedDepartment();
        if (!isHeadOfDepartment(createdBy, department)) {
            throw new ForbiddenActionException(
                    "Only this Department's head Director or an Executive/Super Admin can create its implementation task.");
        }

        boolean hasTeam = request.assignedTeamId() != null;
        boolean hasPerson = request.assignedPersonId() != null;
        if (hasTeam == hasPerson) {
            throw new InvalidAssignmentException("Provide exactly one of assignedTeamId or assignedPersonId.");
        }

        Task task = new Task();
        task.setTaskCode(nextTaskCode());
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setDateAssigned(request.dateAssigned());
        task.setDeadline(request.deadline());
        task.setAssignedBy(createdBy);
        task.setParentTask(parent);
        task.setDepth(parent.getDepth() + 1);
        // No distinct audit value for "a Department's implementation task" — recorded the
        // same way a Director's own subtask creation is, since the authorization tier
        // (Director-or-above) is what CreatedByRole is actually tracking here.
        task.setCreatedByRole(CreatedByRole.DIRECTOR);
        task.setProgressPercentage(0);
        task.setStatus(TaskStatus.PENDING);
        applySourceAndSeverity(task, request.source(), request.sourceLabel(), request.severity(), createdBy);

        if (hasTeam) {
            Team assignedTeam = teamRepository.findById(request.assignedTeamId())
                    .orElseThrow(() -> new ResourceNotFoundException("assignedTeamId not found"));
            if (assignedTeam.getDepartment() == null || !assignedTeam.getDepartment().getId().equals(department.getId())) {
                throw new InvalidAssignmentException("Assigned team must belong to this task's own Department.");
            }
            task.setAssigneeType(AssigneeType.TEAM);
            task.setAssignedTeam(assignedTeam);
        } else {
            Person assignedPerson = personRepository.findById(request.assignedPersonId())
                    .orElseThrow(() -> new ResourceNotFoundException("assignedPersonId not found"));
            if (assignedPerson.getDepartment() == null || !assignedPerson.getDepartment().getId().equals(department.getId())) {
                throw new InvalidAssignmentException("Assigned person must belong to this task's own Department.");
            }
            task.setAssigneeType(AssigneeType.INDIVIDUAL);
            task.setAssignedPerson(assignedPerson);
        }

        Task savedTask = taskRepository.save(task);
        addOpeningComment(savedTask, createdBy, request.openingNote());

        parent.getSubtasks().add(savedTask);
        recalculateParentRollup(parent);
        // Uses the "new task" notification, not notifySubtaskAssigned — this reads as a
        // fresh assignment, not one slice of an already-known team task.
        notificationService.notifyTaskAssigned(savedTask, createdBy);
        recordActivity(savedTask, TaskActivityAction.CREATED, createdBy);

        return taskMapper.toDetailResponse(savedTask);
    }

    private String nextTaskCode() {
        int nextSequence = taskRepository.findMaxTaskCodeSequence() + 1;
        return String.format("TSK-%04d", nextSequence);
    }

    private void addOpeningComment(Task task, Person author, String openingNote) {
        TaskComment comment = new TaskComment();
        comment.setTask(task);
        comment.setAuthor(author);
        comment.setPercentageAtComment(0);
        comment.setBody(openingNote);
        comment.setSequenceNumber(1);
        taskCommentRepository.save(comment);
        task.getComments().add(comment);
    }

    /** Recomputes a task's percentage as the average of its own subtasks (0 if none).
     *  Self-recursive so it bubbles all the way up a 3-level Department hierarchy, not just
     *  one level. Never called for a rolled-up (TEAM-assigned) task's own comments — those
     *  are narrative only. */
    private void recalculateParentRollup(Task parent) {
        List<Task> subtasks = taskRepository.findByParentTaskId(parent.getId());
        int rollup = subtasks.isEmpty()
                ? 0
                : (int) Math.round(subtasks.stream().mapToInt(Task::getProgressPercentage).average().orElse(0.0));
        parent.setProgressPercentage(rollup);
        recalculateStatus(parent);
        parent.setStaleAlertSentAt(null);
        taskRepository.save(parent);

        if (parent.getParentTask() != null) {
            recalculateParentRollup(parent.getParentTask());
        }
    }

    private void recalculateStatus(Task task) {
        if (task.getProgressPercentage() == 0) {
            task.setStatus(TaskStatus.PENDING);
        } else if (task.getProgressPercentage() == 100) {
            task.setStatus(TaskStatus.COMPLETED);
        } else {
            task.setStatus(TaskStatus.ONGOING);
        }
    }

    @Override
    public TaskDetailResponse getTaskById(Long id) {
        Task task = taskRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        return taskMapper.toDetailResponse(task);
    }

    @Override
    public TaskDetailResponse getTaskByCode(String taskCode) {
        Task task = taskRepository.findWithDetailsByTaskCode(taskCode)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        return taskMapper.toDetailResponse(task);
    }

    @Override
    public Page<TaskListResponse> getAllTasks(TaskStatus status, Long assignedPersonId, Long departmentId, Pageable pageable) {
        Pageable pinnedFirst = withPinnedFirst(pageable);
        Page<Task> tasks;
        if (assignedPersonId != null && status != null) {
            tasks = taskRepository.findVisibleToPersonAndStatus(assignedPersonId, status, pinnedFirst);
        } else if (assignedPersonId != null) {
            tasks = taskRepository.findVisibleToPerson(assignedPersonId, pinnedFirst);
        } else if (departmentId != null && status != null) {
            tasks = taskRepository.findByDepartmentIdAndStatus(departmentId, status, pinnedFirst);
        } else if (departmentId != null) {
            tasks = taskRepository.findByDepartmentId(departmentId, pinnedFirst);
        } else if (status != null) {
            // Top-level only — this branch never runs for a Member's own "My Tasks" scope.
            tasks = taskRepository.findByStatusAndParentTaskIsNull(status, pinnedFirst);
        } else {
            tasks = taskRepository.findByParentTaskIsNull(pinnedFirst);
        }
        return tasks.map(t -> {
            TaskComment lastComment = taskCommentRepository.findFirstByTaskIdOrderByCreatedAtDesc(t.getId()).orElse(null);
            return taskMapper.toListResponse(t, lastComment);
        });
    }

    @Override
    public TaskDetailResponse addProgressComment(Long taskId, AddCommentRequest request) {
        Task task = taskRepository.findWithDetailsById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        // Defense-in-depth beyond the DTO's own @Min/@Max — null is a valid, optional
        // narrative-only comment, not a missing value to reject.
        if (request.percentageAtComment() != null
                && (request.percentageAtComment() < 0 || request.percentageAtComment() > 100)) {
            throw new InvalidProgressException("percentageAtComment must be between 0 and 100");
        }

        Person author = personRepository.findById(request.authorId())
                .orElseThrow(() -> new ResourceNotFoundException("Author not found"));

        long commentCount = taskCommentRepository.countByTaskId(taskId);
        // TEAM and DEPARTMENT are always rolled up from children, never set directly.
        boolean isRolledUp = task.getAssigneeType() == AssigneeType.TEAM || task.getAssigneeType() == AssigneeType.DEPARTMENT;
        // Either the task is rolled up, or no percentage was sent — nothing about actual
        // progress changes either way.
        boolean isNarrativeOnly = isRolledUp || request.percentageAtComment() == null;

        TaskComment comment = new TaskComment();
        comment.setTask(task);
        comment.setAuthor(author);
        // A narrative-only comment still needs some percentage on record for the trend
        // chart, so it snapshots the task's current value instead.
        comment.setPercentageAtComment(isNarrativeOnly ? task.getProgressPercentage() : request.percentageAtComment());
        comment.setBody(request.body());
        comment.setSequenceNumber((int) commentCount + 1);
        // Always PROGRESS — a plain Q&A message goes through addDiscussionComment instead.
        comment.setType(CommentType.PROGRESS);

        taskCommentRepository.save(comment);
        task.getComments().add(comment);

        if (isNarrativeOnly) {
            // staleAlertSentAt stays untouched — a note that changes nothing shouldn't
            // quietly look like real progress to the stalled-task check.
        } else {
            // Checking assigneeType (not parentTask == null) is what correctly covers a
            // legacy individually-assigned top-level task too, not just ordinary subtasks.
            task.setProgressPercentage(request.percentageAtComment());
            recalculateStatus(task);
            task.setStaleAlertSentAt(null);
            if (task.getParentTask() != null) {
                recalculateParentRollup(task.getParentTask());
            }
        }

        return taskMapper.toDetailResponse(taskRepository.save(task));
    }

    /** A plain Q&A message, fully open to any authenticated person on any task. Never
     *  touches percentage/status/staleAlertSentAt. Threading is one level deep — a reply to
     *  a reply attaches to its own top-level parent instead of nesting further. */
    @Override
    public TaskDetailResponse addDiscussionComment(Long taskId, AddDiscussionCommentRequest request) {
        Task task = taskRepository.findWithDetailsById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        Person author = personRepository.findById(request.authorId())
                .orElseThrow(() -> new ResourceNotFoundException("Author not found"));

        TaskComment parent = null;
        if (request.parentCommentId() != null) {
            TaskComment requestedParent = taskCommentRepository.findById(request.parentCommentId())
                    .orElseThrow(() -> new ResourceNotFoundException("parentCommentId not found"));
            if (!requestedParent.getTask().getId().equals(taskId)) {
                throw new InvalidAssignmentException("parentCommentId doesn't belong to this task.");
            }
            // Flatten a reply-to-a-reply onto the original top-level comment, exactly like
            // Instagram does, rather than growing a deeper thread.
            parent = requestedParent.getParentComment() != null ? requestedParent.getParentComment() : requestedParent;
        }

        long commentCount = taskCommentRepository.countByTaskId(taskId);

        TaskComment comment = new TaskComment();
        comment.setTask(task);
        comment.setAuthor(author);
        // A snapshot only, for display consistency with PROGRESS comments — never trusted
        // as a real reading (see toTimelineResponse's PROGRESS-only filter, which excludes
        // DISCUSSION comments from the trend chart entirely).
        comment.setPercentageAtComment(task.getProgressPercentage());
        comment.setBody(request.body());
        comment.setSequenceNumber((int) commentCount + 1);
        comment.setType(CommentType.DISCUSSION);
        comment.setParentComment(parent);

        taskCommentRepository.save(comment);
        task.getComments().add(comment);

        if (parent != null) {
            notificationService.notifyDiscussionReplyPosted(comment);
        } else {
            notificationService.notifyDiscussionCommentPosted(comment);
        }

        return taskMapper.toDetailResponse(task);
    }

    @Override
    public TaskDetailResponse reassignTask(Long taskId, ReassignTaskRequest request) {
        Task task = taskRepository.findWithDetailsById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        Person reassignedBy = personRepository.findById(request.reassignedById())
                .orElseThrow(() -> new ResourceNotFoundException("reassignedBy not found"));

        boolean isDepartmentTask = task.getAssigneeType() == AssigneeType.DEPARTMENT;
        if (isDepartmentTask) {
            // Moving a whole Department task is the same authority as creating one — not a
            // plain team-leader-or-Director call.
            if (!Role.isAtLeastExecutive(reassignedBy.getRole())) {
                throw new ForbiddenActionException(
                        "Only an Executive or Super Admin can reassign a Department-level task to a different department.");
            }
        } else {
            requireCanReassign(reassignedBy, task);
        }

        TaskReassignment reassignment = new TaskReassignment();
        reassignment.setTask(task);
        reassignment.setFromAssigneeType(task.getAssigneeType());
        reassignment.setFromPerson(task.getAssignedPerson());
        reassignment.setFromTeam(task.getAssignedTeam());
        reassignment.setFromDepartment(task.getAssignedDepartment());

        // "Top-level-shaped" covers a real depth-0 task and a depth-1 Department
        // implementation task — both staffed freely. Only a true leaf subtask is
        // restricted to reassignSubtask's "same team, individual only" rule.
        boolean isTopLevelShaped = isTopLevelShaped(task);
        if (isDepartmentTask) {
            reassignDepartmentTask(task, request, reassignment);
        } else if (isTopLevelShaped) {
            reassignTopLevelTask(task, request, reassignment);
        } else {
            reassignSubtask(task, request, reassignment);
        }

        reassignment.setReassignedBy(reassignedBy);
        reassignment.setReason(request.reason());
        task.getReassignments().add(reassignment);

        Task savedTask = taskRepository.save(task);
        if (isDepartmentTask) {
            notificationService.notifyDepartmentTaskReassigned(savedTask, reassignment);
        } else if (isTopLevelShaped) {
            notificationService.notifyTaskReassigned(savedTask, reassignment);
        } else {
            notificationService.notifySubtaskReassigned(savedTask, reassignment);
        }

        return taskMapper.toDetailResponse(savedTask);
    }

    /** Moves a Department-level task to a different Department. Never changes assigneeType,
     *  only which Department owns it — its implementation task, if any, is untouched. */
    private void reassignDepartmentTask(Task task, ReassignTaskRequest request, TaskReassignment reassignment) {
        if (request.newDepartmentId() == null) {
            throw new InvalidAssignmentException("newDepartmentId is required to reassign a Department-level task.");
        }
        if (task.getAssignedDepartment() != null && task.getAssignedDepartment().getId().equals(request.newDepartmentId())) {
            throw new InvalidAssignmentException("Task is already assigned to this department.");
        }

        Department newDepartment = departmentRepository.findById(request.newDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("newDepartmentId not found"));

        reassignment.setToAssigneeType(AssigneeType.DEPARTMENT);
        reassignment.setToDepartment(newDepartment);
        task.setAssignedDepartment(newDepartment);
    }

    /** True for a real depth-0 task, or a depth-1 Department implementation task — both
     *  staffed as freely as a brand-new top-level task. False for an ordinary leaf subtask. */
    private boolean isTopLevelShaped(Task task) {
        return task.getParentTask() == null || task.getParentTask().getAssigneeType() == AssigneeType.DEPARTMENT;
    }

    /** Executive/Super Admin always qualifies. A Director only qualifies if they actually
     *  head this department (Department.headDirector), not merely belong to it — the
     *  boundary for every cross-department write action in this class. */
    private boolean isHeadOfDepartment(Person person, Department department) {
        if (Role.isAtLeastExecutive(person.getRole())) {
            return true;
        }
        if (person.getRole() != Role.DIRECTOR) {
            return false;
        }
        return department != null && department.getHeadDirector() != null
                && department.getHeadDirector().getId().equals(person.getId());
    }

    /** A task's own department: assignedDepartment directly, the team's department, or the
     *  assignee's own department — exactly one of these is ever set. */
    private Department resolveTaskDepartment(Task task) {
        return switch (task.getAssigneeType()) {
            case DEPARTMENT -> task.getAssignedDepartment();
            case TEAM -> task.getAssignedTeam() != null ? task.getAssignedTeam().getDepartment() : null;
            case INDIVIDUAL -> task.getAssignedPerson() != null ? task.getAssignedPerson().getDepartment() : null;
        };
    }

    /** Assigning a brand-new top-level task (createTask) or implementation task to a team or
     *  person: Executive/Super Admin may target anyone org-wide, a plain Director only a
     *  team/person within the department they head. */
    private void requireCanAssignWithinDepartment(Person actor, Department targetDepartment) {
        if (!isHeadOfDepartment(actor, targetDepartment)) {
            throw new ForbiddenActionException(
                    "A Director can only assign a task to a team or person within the department they head.");
        }
    }

    /** Called once the caller is already confirmed Director-or-above. A Director may only
     *  delete a task they personally created, within the department they head — a
     *  defense-in-depth check beyond what createTask/createSubtask already enforce.
     *  Executive/Super Admin can always delete anything, regardless of who created it. */
    private void requireCanDelete(Person actor, Task task) {
        if (Role.isAtLeastExecutive(actor.getRole())) {
            return;
        }
        if (!task.getAssignedBy().getId().equals(actor.getId())) {
            throw new ForbiddenActionException("A Director can only delete a task they created themselves.");
        }
        if (!isHeadOfDepartment(actor, resolveTaskDepartment(task))) {
            throw new ForbiddenActionException("A Director can only delete a task within the department they head.");
        }
    }

    /** Only a Director who heads this task's own department, an Executive/Super Admin, or
     *  the leader of the team that currently owns this task, may reassign it — not an
     *  ordinary member, and not a Director from an unrelated department. */
    private void requireCanReassign(Person actor, Task task) {
        if (isHeadOfDepartment(actor, resolveTaskDepartment(task))) {
            return;
        }
        Team owningTeam = isTopLevelShaped(task)
                ? task.getAssignedTeam()
                : task.getParentTask().getAssignedTeam();
        boolean isLeader = owningTeam != null
                && teamMemberRepository.findByTeamIdAndIsLeaderTrue(owningTeam.getId())
                        .map(tm -> tm.getPerson().getId().equals(actor.getId()))
                        .orElse(false);
        if (!isLeader) {
            throw new ForbiddenActionException(
                    "Only a Director who heads this task's department, an Executive/Super Admin, or this team's leader can reassign this task.");
        }
    }

    /** A team-assigned top-level task can only move to a different TEAM; an
     *  individually-assigned top-level task (see CreateTaskRequest) can only move to a
     *  different PERSON — either way, reassignment never changes which of the two a task
     *  is, only who within that lane owns it. */
    private void reassignTopLevelTask(Task task, ReassignTaskRequest request, TaskReassignment reassignment) {
        if (task.getAssigneeType() == AssigneeType.TEAM) {
            if (request.newTeamId() == null) {
                throw new InvalidAssignmentException("newTeamId is required to reassign a team-assigned top-level task.");
            }
            if (task.getAssignedTeam() != null && task.getAssignedTeam().getId().equals(request.newTeamId())) {
                throw new InvalidAssignmentException("Task is already assigned to this team.");
            }

            Team newTeam = teamRepository.findById(request.newTeamId())
                    .orElseThrow(() -> new ResourceNotFoundException("newTeamId not found"));

            reassignment.setToAssigneeType(AssigneeType.TEAM);
            reassignment.setToTeam(newTeam);
            task.setAssignedTeam(newTeam);
            task.setAssignedPerson(null);
            task.setAssigneeType(AssigneeType.TEAM);
        } else {
            if (request.newPersonId() == null) {
                throw new InvalidAssignmentException("newPersonId is required to reassign an individually-assigned top-level task.");
            }
            if (task.getAssignedPerson() != null && task.getAssignedPerson().getId().equals(request.newPersonId())) {
                throw new InvalidAssignmentException("Task is already assigned to this person.");
            }

            Person newPerson = personRepository.findById(request.newPersonId())
                    .orElseThrow(() -> new ResourceNotFoundException("newPersonId not found"));

            reassignment.setToAssigneeType(AssigneeType.INDIVIDUAL);
            reassignment.setToPerson(newPerson);
            task.setAssignedPerson(newPerson);
            task.setAssignedTeam(null);
            task.setAssigneeType(AssigneeType.INDIVIDUAL);
        }
    }

    /** A subtask can only move to a different INDIVIDUAL who is a member of the SAME team
     *  that owns its parent task — never to a different team, never to the team itself. */
    private void reassignSubtask(Task task, ReassignTaskRequest request, TaskReassignment reassignment) {
        if (request.newPersonId() == null) {
            throw new InvalidAssignmentException("newPersonId is required to reassign a subtask.");
        }
        if (task.getAssignedPerson() != null && task.getAssignedPerson().getId().equals(request.newPersonId())) {
            throw new InvalidAssignmentException("Task is already assigned to this person.");
        }

        Long teamId = task.getParentTask().getAssignedTeam().getId();
        if (!teamMemberRepository.existsByTeamIdAndPersonId(teamId, request.newPersonId())) {
            throw new InvalidAssignmentException("New assignee must be a member of the parent task's team.");
        }

        Person newPerson = personRepository.findById(request.newPersonId())
                .orElseThrow(() -> new ResourceNotFoundException("newPersonId not found"));

        reassignment.setToAssigneeType(AssigneeType.INDIVIDUAL);
        reassignment.setToPerson(newPerson);
        task.setAssignedPerson(newPerson);
        task.setAssignedTeam(null);
        task.setAssigneeType(AssigneeType.INDIVIDUAL);
    }

    @Override
    public Page<TaskTimelineResponse> getTaskProgressTimeline(Long taskId, Pageable pageable) {
        return taskCommentRepository.findByTaskIdOrderByCreatedAtAsc(taskId, pageable)
                .map(taskMapper::toTimelineResponse);
    }

    @Override
    public Page<CommentResponse> getTaskComments(Long taskId, Pageable pageable) {
        return taskCommentRepository.findByTaskIdOrderByCreatedAtAsc(taskId, pageable)
                .map(taskMapper::toCommentResponse);
    }

    @Override
    public Page<ReassignmentResponse> getTaskReassignments(Long taskId, Pageable pageable) {
        return taskReassignmentRepository.findByTaskIdOrderByReassignedAtAsc(taskId, pageable)
                .map(taskMapper::toReassignmentResponse);
    }

    @Override
    public Page<TaskListResponse> searchTasks(String q, Long assignedPersonId, Long departmentId, Pageable pageable) {
        Pageable pinnedFirst = withPinnedFirst(pageable);
        Page<Task> results = assignedPersonId != null
                ? taskRepository.searchVisibleToPerson(q, assignedPersonId, pinnedFirst)
                : departmentId != null
                        ? taskRepository.searchByDepartmentId(q, departmentId, pinnedFirst)
                        : taskRepository.search(q, pinnedFirst);
        return results.map(t -> {
            TaskComment lastComment = taskCommentRepository.findFirstByTaskIdOrderByCreatedAtDesc(t.getId()).orElse(null);
            return taskMapper.toListResponse(t, lastComment);
        });
    }

    @Override
    public TaskDetailResponse updateTask(Long id, UpdateTaskRequest request) {
        Task task = taskRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        requireReasonableDate(request.dateAssigned());

        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setDateAssigned(request.dateAssigned());

        return taskMapper.toDetailResponse(taskRepository.save(task));
    }

    /** Bounds dateAssigned: no more than a year ahead (typo guard), and no more than 3
     *  months in the past (real backfilling fits that; anything older is likely a mistyped
     *  date). The authoritative check — mirrors the frontend's own date-picker limits. */
    private void requireReasonableDate(LocalDate dateAssigned) {
        if (dateAssigned.isAfter(LocalDate.now().plusYears(1))) {
            throw new InvalidAssignmentException("dateAssigned can't be more than a year in the future.");
        }
        if (dateAssigned.isBefore(LocalDate.now().minusMonths(3))) {
            throw new InvalidAssignmentException("dateAssigned can't be more than 3 months in the past.");
        }
    }

    /** A deadline before the task starts makes no sense — checked once at creation. Never
     *  re-checked afterward, since the extension workflow only ever moves it later. */
    private void requireDeadlineNotBeforeAssignment(LocalDate dateAssigned, LocalDate deadline) {
        if (deadline.isBefore(dateAssigned)) {
            throw new InvalidAssignmentException("deadline can't be before dateAssigned.");
        }
    }

    /** source/sourceLabel are open to any creator. severity is Executive-only — even a
     *  Director creating a task under a CRITICAL Department root can't set it. CRITICAL
     *  auto-pins once, at creation only; pinning stays independently editable afterward. */
    private void applySourceAndSeverity(Task task, TaskSource source, String sourceLabel, TaskSeverity severity, Person createdBy) {
        task.setSource(source);
        task.setSourceLabel(sourceLabel);
        if (severity != null) {
            if (!Role.isAtLeastExecutive(createdBy.getRole())) {
                throw new ForbiddenActionException("Only an Executive or Super Admin can set a task's severity.");
            }
            task.setSeverity(severity);
            task.setPinned(severity == TaskSeverity.CRITICAL);
        }
    }

    /** Composes "pinned first" as an extra leading sort key onto whatever the client
     *  already requested (or nothing, for the "none" sort case) — transparent under
     *  Newest/Recently-updated/All exactly as those already work, rather than a 4th
     *  client-facing sort option or duplicated repository queries. */
    private Pageable withPinnedFirst(Pageable pageable) {
        Sort pinnedFirst = Sort.by(Sort.Direction.DESC, "pinned").and(pageable.getSort());
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), pinnedFirst);
    }

    @Override
    public void deleteTask(Long id, Long actorId) {
        Person actor = personRepository.findById(actorId)
                .orElseThrow(() -> new ResourceNotFoundException("actorId not found"));
        if (!Role.isAtLeastDirector(actor.getRole())) {
            throw new ForbiddenActionException("Only a Director or Super Admin can delete a task.");
        }

        Task task = taskRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        requireCanDelete(actor, task);

        Task parent = task.getParentTask();

        // Both recorded before the delete — they read taskCode/title off the entity, which
        // won't exist to read from once it's gone. Only this one task gets a log entry, not
        // the whole cascade it takes with it.
        recordActivity(task, TaskActivityAction.DELETED, actor);
        notificationService.notifyTaskDeleted(task, actor);

        // Cascades to subtasks (Task.subtasks is CascadeType.ALL + orphanRemoval). Deleting
        // a subtask needs the parent's rollup recomputed afterward.
        taskRepository.delete(task);

        if (parent != null) {
            recalculateParentRollup(parent);
        }
    }

    private void recordActivity(Task task, TaskActivityAction action, Person performedBy) {
        TaskActivity activity = new TaskActivity();
        activity.setAction(action);
        activity.setTaskCode(task.getTaskCode());
        activity.setTitle(task.getTitle());
        activity.setParentTaskCode(task.getParentTask() != null ? task.getParentTask().getTaskCode() : null);
        activity.setParentTaskTitle(task.getParentTask() != null ? task.getParentTask().getTitle() : null);
        activity.setAssigneeType(task.getAssigneeType());
        activity.setAssigneeSummary(switch (task.getAssigneeType()) {
            case TEAM -> task.getAssignedTeam().getName();
            case INDIVIDUAL -> task.getAssignedPerson().getFullName();
            case DEPARTMENT -> task.getAssignedDepartment().getName();
        });
        activity.setPerformedBy(performedBy);
        taskActivityRepository.save(activity);
    }

    @Override
    public Page<TaskActivityResponse> getTaskActivity(Long requesterId, Pageable pageable) {
        Person requester = personRepository.findById(requesterId)
                .orElseThrow(() -> new ResourceNotFoundException("requesterId not found"));
        if (!Role.isAtLeastDirector(requester.getRole())) {
            throw new ForbiddenActionException("Only a Director or Super Admin can view task activity.");
        }

        return taskActivityRepository.findAll(pageable).map(a -> new TaskActivityResponse(
                a.getId(),
                a.getAction(),
                a.getTaskCode(),
                a.getTitle(),
                a.getParentTaskCode(),
                a.getParentTaskTitle(),
                a.getAssigneeType(),
                a.getAssigneeSummary(),
                a.getPerformedBy().getFullName(),
                a.getTimestamp()
        ));
    }

    @Override
    public TaskDetailResponse requestDeadlineExtension(Long taskId, RequestDeadlineExtensionRequest request) {
        Task task = taskRepository.findWithDetailsById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        Person requestedBy = personRepository.findById(request.requestedById())
                .orElseThrow(() -> new ResourceNotFoundException("requestedById not found"));
        requireCanRequestExtension(requestedBy, task);

        if (task.getDeadline() != null && !request.requestedDeadline().isAfter(task.getDeadline())) {
            throw new InvalidAssignmentException("requestedDeadline must be after the task's current deadline.");
        }

        TaskDeadlineExtensionRequest extensionRequest = new TaskDeadlineExtensionRequest();
        extensionRequest.setTask(task);
        extensionRequest.setCurrentDeadline(task.getDeadline());
        extensionRequest.setRequestedDeadline(request.requestedDeadline());
        extensionRequest.setJustification(request.justification());
        extensionRequest.setRequestedBy(requestedBy);
        extensionRequest.setStatus(ExtensionRequestStatus.PENDING);

        TaskDeadlineExtensionRequest saved = taskDeadlineExtensionRequestRepository.save(extensionRequest);
        task.getDeadlineExtensionRequests().add(saved);

        notificationService.notifyDeadlineExtensionRequested(saved);

        return taskMapper.toDetailResponse(task);
    }

    @Override
    public TaskDetailResponse decideDeadlineExtension(Long taskId, Long extensionRequestId, DecideDeadlineExtensionRequest request) {
        Task task = taskRepository.findWithDetailsById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        TaskDeadlineExtensionRequest extensionRequest = taskDeadlineExtensionRequestRepository.findById(extensionRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Deadline extension request not found"));
        if (!extensionRequest.getTask().getId().equals(taskId)) {
            throw new InvalidAssignmentException("This extension request doesn't belong to this task.");
        }
        if (extensionRequest.getStatus() != ExtensionRequestStatus.PENDING) {
            throw new InvalidAssignmentException("This extension request has already been decided.");
        }

        Person decidedBy = personRepository.findById(request.decidedById())
                .orElseThrow(() -> new ResourceNotFoundException("decidedById not found"));
        if (request.approve()) {
            requireCanApprove(decidedBy, task);
        } else {
            requireCanReject(decidedBy, task);
        }

        extensionRequest.setStatus(request.approve() ? ExtensionRequestStatus.APPROVED : ExtensionRequestStatus.REJECTED);
        extensionRequest.setDecidedBy(decidedBy);
        extensionRequest.setDecisionNote(request.decisionNote());
        extensionRequest.setDecidedAt(LocalDateTime.now());
        taskDeadlineExtensionRequestRepository.save(extensionRequest);

        if (request.approve()) {
            task.setDeadline(extensionRequest.getRequestedDeadline());
            taskRepository.save(task);
            notificationService.notifyDeadlineExtensionApproved(extensionRequest);
        } else {
            notificationService.notifyDeadlineExtensionRejected(extensionRequest);
        }

        return taskMapper.toDetailResponse(task);
    }

    @Override
    public TaskDetailResponse forwardExtensionRequestToApprover(Long taskId, Long extensionRequestId, Long forwardedById) {
        Task task = taskRepository.findWithDetailsById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        TaskDeadlineExtensionRequest extensionRequest = taskDeadlineExtensionRequestRepository.findById(extensionRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Deadline extension request not found"));
        if (!extensionRequest.getTask().getId().equals(taskId)) {
            throw new InvalidAssignmentException("This extension request doesn't belong to this task.");
        }
        if (extensionRequest.getStatus() != ExtensionRequestStatus.PENDING) {
            throw new InvalidAssignmentException("This extension request has already been decided.");
        }
        if (extensionRequest.getForwardedAt() != null) {
            throw new InvalidAssignmentException("This extension request has already been sent to the CEO.");
        }
        if (!isCeoMandated(task)) {
            throw new InvalidAssignmentException(
                    "This task doesn't need the CEO's approval — you can decide it directly.");
        }

        Person forwardedBy = personRepository.findById(forwardedById)
                .orElseThrow(() -> new ResourceNotFoundException("forwardedById not found"));
        // Same authority as rejecting: whoever the request landed on to decide has standing
        // to send it onward, even though only the CEO/Super Admin can actually grant it.
        requireCanReject(forwardedBy, task);

        extensionRequest.setForwardedBy(forwardedBy);
        extensionRequest.setForwardedAt(LocalDateTime.now());
        taskDeadlineExtensionRequestRepository.save(extensionRequest);

        notificationService.notifyDeadlineExtensionForwarded(extensionRequest, forwardedBy);

        return taskMapper.toDetailResponse(task);
    }

    @Override
    public TaskDetailResponse extendDeadlineDirectly(Long taskId, ExtendDeadlineRequest request) {
        Task task = taskRepository.findWithDetailsById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        Person extendedBy = personRepository.findById(request.extendedById())
                .orElseThrow(() -> new ResourceNotFoundException("extendedById not found"));
        // An immediate, self-approved extension — same authority as approving a pending
        // request (see requireCanApprove): a Director can't grant themselves one on a
        // CEO-mandated task any more than they could approve someone else's request on it.
        requireCanApprove(extendedBy, task);

        if (task.getDeadline() != null && !request.newDeadline().isAfter(task.getDeadline())) {
            throw new InvalidAssignmentException("newDeadline must be after the task's current deadline.");
        }

        LocalDate previousDeadline = task.getDeadline();
        String justification = request.reason() != null && !request.reason().isBlank()
                ? request.reason() : "Extended directly, no request/approval round-trip.";

        TaskDeadlineExtensionRequest extensionRequest = new TaskDeadlineExtensionRequest();
        extensionRequest.setTask(task);
        extensionRequest.setCurrentDeadline(previousDeadline);
        extensionRequest.setRequestedDeadline(request.newDeadline());
        extensionRequest.setJustification(justification);
        extensionRequest.setRequestedBy(extendedBy);
        extensionRequest.setStatus(ExtensionRequestStatus.APPROVED);
        extensionRequest.setDecidedBy(extendedBy);
        extensionRequest.setDecisionNote(request.reason());
        extensionRequest.setDecidedAt(LocalDateTime.now());

        TaskDeadlineExtensionRequest saved = taskDeadlineExtensionRequestRepository.save(extensionRequest);
        task.getDeadlineExtensionRequests().add(saved);
        task.setDeadline(request.newDeadline());

        Task savedTask = taskRepository.save(task);
        notificationService.notifyDeadlineExtended(savedTask, previousDeadline, extendedBy);

        return taskMapper.toDetailResponse(savedTask);
    }

    @Override
    public Page<DeadlineExtensionResponse> getDeadlineHistory(Long taskId, Pageable pageable) {
        return taskDeadlineExtensionRequestRepository.findByTaskIdOrderByRequestedAtDesc(taskId, pageable)
                .map(taskMapper::toDeadlineExtensionResponse);
    }

    @Override
    public List<PendingExtensionRequestResponse> getPendingExtensionRequests(Long viewerId) {
        Person viewer = personRepository.findById(viewerId)
                .orElseThrow(() -> new ResourceNotFoundException("viewerId not found"));

        List<TaskDeadlineExtensionRequest> pending =
                taskDeadlineExtensionRequestRepository.findByStatusOrderByRequestedAtDesc(ExtensionRequestStatus.PENDING);

        // A Super Admin never naturally resolves as anyone's decider/approver (they don't
        // create tasks in the normal flow), so this inbox would always read empty for them
        // despite their real override authority — show every pending request instead.
        if (viewer.getRole() == Role.SUPER_ADMIN) {
            return pending.stream()
                    .map(request -> taskMapper.toPendingExtensionResponse(request, true))
                    .toList();
        }

        // The rejecter sees it immediately; the approver only once it's explicitly
        // forwarded (see forwardExtensionRequestToApprover) — keeps the CEO's inbox from
        // filling with every request automatically on a CEO-mandated chain. On an ordinary
        // Director-originated chain, rejecter and approver are the same person either way.
        return pending.stream()
                .filter(request -> {
                    Task task = request.getTask();
                    boolean isDecider = resolveDeadlineDecider(task).getId().equals(viewerId);
                    boolean isApprover = resolveDeadlineApprover(task).getId().equals(viewerId);
                    return isDecider || (isApprover && request.getForwardedAt() != null);
                })
                // Tells the frontend which action(s) this viewer actually has, so a
                // rejecter-only Director doesn't see an Approve button that would just fail.
                .map(request -> taskMapper.toPendingExtensionResponse(request, canApproveDeadline(viewer, request.getTask())))
                .toList();
    }

    @Override
    public TaskDetailResponse setPinned(Long taskId, SetPinnedRequest request) {
        Task task = taskRepository.findWithDetailsById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        Person changedBy = personRepository.findById(request.changedById())
                .orElseThrow(() -> new ResourceNotFoundException("changedById not found"));
        if (!isHeadOfDepartment(changedBy, resolveTaskDepartment(task))) {
            throw new ForbiddenActionException(
                    "Only a Director who heads this task's department (or an Executive/Super Admin) can pin or unpin it.");
        }

        task.setPinned(request.pinned());
        return taskMapper.toDetailResponse(taskRepository.save(task));
    }

    private static final long MAX_DOCUMENT_SIZE_BYTES = 20L * 1024 * 1024;

    // Common office/document/image types a "supporting document" for a task realistically
    // is — not an arbitrary-file upload. Easy to extend later (a plain Set, no enum/CHECK
    // constraint involved).
    private static final Set<String> ALLOWED_DOCUMENT_CONTENT_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "image/png",
            "image/jpeg",
            "text/plain"
    );

    @Override
    public TaskDetailResponse addDocument(Long taskId, String fileName, String contentType, byte[] content, Long uploadedById) {
        Task task = taskRepository.findWithDetailsById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        Person uploadedBy = personRepository.findById(uploadedById)
                .orElseThrow(() -> new ResourceNotFoundException("uploadedById not found"));

        if (content == null || content.length == 0) {
            throw new InvalidAssignmentException("The uploaded file is empty.");
        }
        if (content.length > MAX_DOCUMENT_SIZE_BYTES) {
            throw new InvalidAssignmentException("A supporting document can't be larger than 20MB.");
        }
        if (contentType == null || !ALLOWED_DOCUMENT_CONTENT_TYPES.contains(contentType)) {
            throw new InvalidAssignmentException(
                    "Unsupported file type — only PDF, Word, Excel, PowerPoint, PNG/JPEG images, and plain text are allowed.");
        }

        TaskDocument document = new TaskDocument();
        document.setTask(task);
        document.setFileName(fileName != null && !fileName.isBlank() ? fileName : "Untitled file");
        document.setContentType(contentType);
        document.setFileSize(content.length);
        document.setContent(content);
        document.setUploadedBy(uploadedBy);

        TaskDocument saved = taskDocumentRepository.save(document);
        task.getDocuments().add(saved);

        return taskMapper.toDetailResponse(task);
    }

    @Override
    public DocumentDownload getDocumentContent(Long taskId, Long documentId) {
        TaskDocument document = taskDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
        if (!document.getTask().getId().equals(taskId)) {
            throw new InvalidAssignmentException("This document doesn't belong to this task.");
        }
        return new DocumentDownload(document.getFileName(), document.getContentType(), document.getContent());
    }

    @Override
    public TaskDetailResponse deleteDocument(Long taskId, Long documentId, Long actorId) {
        Task task = taskRepository.findWithDetailsById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        TaskDocument document = taskDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
        if (!document.getTask().getId().equals(taskId)) {
            throw new InvalidAssignmentException("This document doesn't belong to this task.");
        }
        Person actor = personRepository.findById(actorId)
                .orElseThrow(() -> new ResourceNotFoundException("actorId not found"));

        boolean isUploader = document.getUploadedBy().getId().equals(actorId);
        if (!isUploader && !Role.isAtLeastDirector(actor.getRole())) {
            throw new ForbiddenActionException(
                    "Only whoever uploaded this document, or a Director/Executive/Super Admin, can remove it.");
        }

        task.getDocuments().remove(document);
        taskDocumentRepository.delete(document);

        return taskMapper.toDetailResponse(task);
    }

    /** Same "who's actually responsible for this task" concept used by TaskStalenessJob/
     *  notifyTaskAssigned: the team's Leader for a TEAM-assigned task, the assignee for an
     *  INDIVIDUAL one, or the Department's head Director for a DEPARTMENT-assigned one.
     *  Null if there's nobody to resolve to (e.g. a team with no leader set). */
    private Person resolveAccountablePerson(Task task) {
        return switch (task.getAssigneeType()) {
            case INDIVIDUAL -> task.getAssignedPerson();
            case TEAM -> task.getAssignedTeam() != null
                    ? teamMemberRepository.findByTeamIdAndIsLeaderTrue(task.getAssignedTeam().getId())
                            .map(TeamMember::getPerson).orElse(null)
                    : null;
            case DEPARTMENT -> task.getAssignedDepartment() != null ? task.getAssignedDepartment().getHeadDirector() : null;
        };
    }

    /** The override tier for anything deadline-related on this task: Executive-or-above
     *  for a Department-assigned task (the same authority that assigns/reassigns one),
     *  Director-or-above otherwise — mirrors reassignTask's own split. */
    private boolean isDeadlineOverrideTier(Task task, Person actor) {
        return task.getAssigneeType() == AssigneeType.DEPARTMENT
                ? Role.isAtLeastExecutive(actor.getRole())
                : Role.isAtLeastDirector(actor.getRole());
    }

    /** Requesting an extension: the task's own accountable person (Team Leader/individual
     *  assignee/Department head Director), or the override tier. */
    private void requireCanRequestExtension(Person actor, Task task) {
        if (isDeadlineOverrideTier(task, actor)) {
            return;
        }
        Person accountable = resolveAccountablePerson(task);
        if (accountable == null || !accountable.getId().equals(actor.getId())) {
            throw new ForbiddenActionException("Only this task's accountable person, or a Director/Super Admin "
                    + "(Executive/Super Admin for a Department task), can request a deadline extension.");
        }
    }

    /** Rejecting: this task's own deadline decider, or the override tier. Never escalated
     *  further, even on a CEO-mandated chain — a "no" doesn't need the CEO's sign-off. */
    private boolean canRejectDeadline(Person actor, Task task) {
        return isDeadlineOverrideTier(task, actor) || resolveDeadlineDecider(task).getId().equals(actor.getId());
    }

    private void requireCanReject(Person actor, Task task) {
        if (!canRejectDeadline(actor, task)) {
            throw new ForbiddenActionException("Only whoever set this task's deadline, or a Director/Super Admin "
                    + "(Executive/Super Admin for a Department task), can decide a deadline extension.");
        }
    }

    /** Approving (or a direct extension — an implicit, immediate approval): on a
     *  CEO-mandated chain, only Executive-or-above may grant it, no matter how far down the
     *  hierarchy or who created this particular task — a Director can still reject such a
     *  request, just not approve it. Otherwise the same authority as rejecting. Also backs
     *  the "Requests" inbox's canApprove flag, so it can hide an Approve button that would
     *  just fail. */
    private boolean canApproveDeadline(Person actor, Task task) {
        if (isCeoMandated(task) && !Role.isAtLeastExecutive(actor.getRole())) {
            return false;
        }
        return canRejectDeadline(actor, task);
    }

    private void requireCanApprove(Person actor, Task task) {
        if (!canApproveDeadline(actor, task)) {
            throw new ForbiddenActionException("This task originated from the CEO's own mandate — only an "
                    + "Executive or Super Admin can approve a deadline extension on it.");
        }
    }

    /** Walks up to this task's own root (depth 0 — the top of whatever hierarchy it sits
     *  in). Every hierarchy is rooted in something a Director-or-above created (see
     *  createTask's own role floor), so this always terminates. */
    private Task findRoot(Task task) {
        Task current = task;
        while (current.getParentTask() != null) {
            current = current.getParentTask();
        }
        return current;
    }

    /** True when this task's hierarchy originates from an Executive/Super Admin's own
     *  mandate (a Department task, not an ordinary Director-created one). Everything below
     *  that root inherits the same "only the CEO actually approves" rule. */
    private boolean isCeoMandated(Task task) {
        return Role.isAtLeastExecutive(findRoot(task).getAssignedBy().getRole());
    }

    /** The true approving authority: on a CEO-mandated chain, the root's own assignedBy
     *  (the CEO), even though a Director further down is who the request first lands on and
     *  resolves as its rejecter. Identical to resolveDeadlineDecider otherwise. */
    private Person resolveDeadlineApprover(Task task) {
        Task root = findRoot(task);
        if (Role.isAtLeastExecutive(root.getAssignedBy().getRole())) {
            return root.getAssignedBy();
        }
        return resolveDeadlineDecider(task);
    }

    /** Deadline decisions are a Director's job, not a Team Leader's, even though a Team
     *  Leader can be a leaf subtask's own assignedBy. Uses this task's own assignedBy if
     *  already Director-or-above, else walks up to the nearest ancestor whose creator is. */
    private Person resolveDeadlineDecider(Task task) {
        Task current = task;
        while (current != null) {
            if (Role.isAtLeastDirector(current.getAssignedBy().getRole())) {
                return current.getAssignedBy();
            }
            current = current.getParentTask();
        }
        // Unreachable — every hierarchy is rooted in a Director-or-above-created task.
        return task.getAssignedBy();
    }
}
