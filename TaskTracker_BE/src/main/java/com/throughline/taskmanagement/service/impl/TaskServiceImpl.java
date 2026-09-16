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
import com.throughline.taskmanagement.model.TaskReassignment;
import com.throughline.taskmanagement.model.Team;
import com.throughline.taskmanagement.model.TeamMember;
import com.throughline.taskmanagement.repository.DepartmentRepository;
import com.throughline.taskmanagement.repository.PersonRepository;
import com.throughline.taskmanagement.repository.TaskActivityRepository;
import com.throughline.taskmanagement.repository.TaskCommentRepository;
import com.throughline.taskmanagement.repository.TaskDeadlineExtensionRequestRepository;
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
            task.setAssigneeType(AssigneeType.TEAM);
            task.setAssignedTeam(assignedTeam);
        } else if (hasPerson) {
            Person assignedPerson = personRepository.findById(request.assignedPersonId())
                    .orElseThrow(() -> new ResourceNotFoundException("assignedPersonId not found"));
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

        // A DEPARTMENT-typed parent is always depth 0 (only an Executive creates one) — its
        // child is the new depth-1 "implementation task", team- or individual-assigned, org-
        // wide. Every other parent shape (a plain top-level TEAM task, or a depth-1 TEAM
        // implementation task) uses the original leaf-subtask case: always INDIVIDUAL,
        // always a member of the parent's own team. An INDIVIDUAL-typed parent is never a
        // valid parent at all — falls through to the "no assigned team" rejection below,
        // same as before this phase.
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

        // isDirector here also covers Super Admin (see Role.isAtLeastDirector) — a
        // Super-Admin-created subtask is recorded as CreatedByRole.DIRECTOR below, same as
        // a Director's, rather than adding a third audit value just for this.
        boolean isDirector = Role.isAtLeastDirector(createdBy.getRole());
        boolean isThisTeamsLeader = teamMemberRepository.findByTeamIdAndPersonId(teamId, createdBy.getId())
                .map(TeamMember::isLeader)
                .orElse(false);
        if (!isDirector && !isThisTeamsLeader) {
            throw new ForbiddenActionException("Only the parent task's Team Leader or a Director can create a subtask.");
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
        subtask.setCreatedByRole(isDirector ? CreatedByRole.DIRECTOR : CreatedByRole.TEAM_LEADER);
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

    /** The new depth-1 case: parent is a Department-assigned Executive task. Its
     *  "implementation task" is team- or individual-assigned, exactly like a plain
     *  top-level task's own creation (see createTask) — no team-membership restriction,
     *  since this is the Department turning an org-wide mandate into real work, not a Team
     *  Leader staffing their own roster. Restricted to that Department's own head Director
     *  (or an Executive/Super Admin override) — not just any Director, since a Director
     *  who doesn't head this Department has no standing over its Executive-level mandate. */
    private TaskDetailResponse createImplementationTask(Task parent, CreateSubtaskRequest request, Person createdBy) {
        Department department = parent.getAssignedDepartment();
        boolean isExecutiveOrAbove = Role.isAtLeastExecutive(createdBy.getRole());
        boolean isThisDepartmentsHead = department.getHeadDirector() != null
                && department.getHeadDirector().getId().equals(createdBy.getId());
        if (!isExecutiveOrAbove && !isThisDepartmentsHead) {
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
        // Reuses the plain "you were handed a new task" notification, not
        // notifySubtaskAssigned — this reads to its recipient exactly like a fresh
        // top-level assignment (a whole team or a single person taking on new work), not
        // like being handed one slice of an already-known team task.
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

    /** Recomputes a task's percentage as the average of its own subtasks (0 if none),
     *  called whenever one of them changes. Self-recursive: a depth-1 task under a
     *  Department root can itself have a parent (the depth-0 Department task), which must
     *  bubble the same way once this task's own rollup changes — nothing guarantees exactly
     *  one depth-1 child per Department task, so genuine average-of-children rollup all the
     *  way up is the only correct behavior. For the plain 2-level case this is a no-op
     *  beyond the first call, since a depth-0 task has no parent of its own. Never called
     *  for a comment added directly to a task that's rolled-up (TEAM-assigned) — that's
     *  narrative only. */
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
            // Top-level tasks only, same reasoning as findByDepartmentId/AndStatus just
            // above — this branch (and the plain findByParentTaskIsNull one right below)
            // only ever runs when assignedPersonId is absent, i.e. never for a Member's own
            // "My Tasks", where hiding subtasks would often leave them with nothing to see.
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

        // Defense-in-depth: @Min/@Max on AddCommentRequest already reject an out-of-range
        // percentage at the HTTP boundary, but this is the one place progress is actually
        // written, so it must not trust the DTO alone. Skipped entirely when null — that's
        // a valid, genuinely optional narrative-only comment, not a missing value to reject.
        if (request.percentageAtComment() != null
                && (request.percentageAtComment() < 0 || request.percentageAtComment() > 100)) {
            throw new InvalidProgressException("percentageAtComment must be between 0 and 100");
        }

        Person author = personRepository.findById(request.authorId())
                .orElseThrow(() -> new ResourceNotFoundException("Author not found"));

        long commentCount = taskCommentRepository.countByTaskId(taskId);
        // TEAM and DEPARTMENT are both rolled up from children, never set by a comment
        // directly — a DEPARTMENT task always has children (its implementation task), same
        // reasoning as a plain team task's subtasks.
        boolean isRolledUp = task.getAssigneeType() == AssigneeType.TEAM || task.getAssigneeType() == AssigneeType.DEPARTMENT;
        // A narrative-only comment — no percentage sent at all — changes nothing about
        // actual progress, whether that's because the task is rolled up (percentage is
        // never trusted from a comment either way) or because the commenter explicitly
        // chose to leave a note without logging a change.
        boolean isNarrativeOnly = isRolledUp || request.percentageAtComment() == null;

        TaskComment comment = new TaskComment();
        comment.setTask(task);
        comment.setAuthor(author);
        // A narrative-only comment still needs SOME percentage on record for the trend/
        // timeline chart, so it gets the task's own current progress instead of an
        // arbitrary number, or one the commenter never actually supplied.
        comment.setPercentageAtComment(isNarrativeOnly ? task.getProgressPercentage() : request.percentageAtComment());
        comment.setBody(request.body());
        comment.setSequenceNumber((int) commentCount + 1);
        // Always PROGRESS — this endpoint is the progress log specifically; a plain
        // question/discussion message goes through addDiscussionComment instead, which
        // never touches percentage/status at all (see CommentType).
        comment.setType(CommentType.PROGRESS);

        taskCommentRepository.save(comment);
        task.getComments().add(comment);

        if (isNarrativeOnly) {
            // No change to progress/status, and staleAlertSentAt is deliberately left
            // untouched — a note that changes nothing shouldn't quietly defeat the
            // stalled-task check by looking like real activity.
        } else {
            // Individually-assigned: a normal subtask (parentTask != null), or a task from
            // before the hierarchy rework that's individually-assigned at the top level
            // (parentTask == null but never got migrated to a real subtask) — either way,
            // the comment's percentage IS this task's real progress. Checking assigneeType
            // here rather than "parentTask == null" is what makes that legacy case work:
            // the old check treated any parentless task as team-only/narrative-only, so a
            // legacy individual task's comments silently never moved its percentage.
            task.setProgressPercentage(request.percentageAtComment());
            recalculateStatus(task);
            task.setStaleAlertSentAt(null);
            if (task.getParentTask() != null) {
                recalculateParentRollup(task.getParentTask());
            }
        }

        return taskMapper.toDetailResponse(taskRepository.save(task));
    }

    /** A plain Q&A message — fully open (any authenticated person may post on any task,
     *  same as the progress log always has been; see the plan's confirmed decision to keep
     *  discussion visibility/authorship completely open rather than restricting it to a
     *  private Executive-Director channel). Never touches percentage/status/
     *  staleAlertSentAt. Threading is one level deep, same as Instagram: replying to a
     *  reply attaches to that reply's own top-level parent instead of nesting further. */
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
            // A different, higher tier than the team-leader-or-Director rule below — moving
            // a whole Department task to a different Department is the same authority as
            // creating one in the first place (see createTask), not something a Director
            // who happens to lead a team gets a say in.
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

        // "Top-level-shaped" covers a true depth-0 task AND a depth-1 implementation task
        // under a Department root — both are staffed the same free-form way (team OR
        // individual, no membership restriction). Only a real leaf subtask (depth 1 under a
        // plain team task, or depth 2 under a Department's team-assigned implementation
        // task) is restricted to reassignSubtask's "same team, individual only" rule.
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

    /** Moves a Department-level task (always depth 0) to a different Department entirely —
     *  e.g. the CEO's office realizes what was handed to IT actually belongs to
     *  Cybersecurity. Never changes assigneeType (stays DEPARTMENT), only which Department
     *  owns it — its (not yet created, or already-created) implementation task is
     *  untouched by this, same as reassigning a top-level task never touches its subtasks. */
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

    /** True for a real depth-0 task, and for a depth-1 implementation task directly under a
     *  Department root — both are staffed the same free-form way as a brand-new top-level
     *  task. False for an ordinary leaf subtask, which is restricted to one team. */
    private boolean isTopLevelShaped(Task task) {
        return task.getParentTask() == null || task.getParentTask().getAssigneeType() == AssigneeType.DEPARTMENT;
    }

    /** Only a Director/Super Admin, or the leader of the team that currently owns this task,
     *  may reassign it — an ordinary team member cannot. "The team that currently owns this
     *  task" is the task's OWN team when it's top-level-shaped (a real depth-0 task, or a
     *  depth-1 Department implementation task — see isTopLevelShaped), or its parent's team
     *  for an ordinary leaf subtask. */
    /** Deleting is narrower than "any Director" — called only once the caller is already
     *  confirmed Director-or-above (see deleteTask's own role floor). A Director may only
     *  delete a task they personally created (assignedBy == them): their own top-level
     *  task, or a subtask/implementation task they added underneath something else. A task
     *  an Executive/Super Admin created — most notably a Department-assigned task, always
     *  their own doing — can only be deleted by Executive-or-above, even by the Director
     *  whose own department it was handed to; the receiving Director isn't its creator,
     *  just its recipient. Executive/Super Admin can always delete anything, regardless of
     *  who created it — the same override tier used everywhere else in this app
     *  (reassignment, deadline decisions). */
    private void requireCanDelete(Person actor, Task task) {
        if (Role.isAtLeastExecutive(actor.getRole())) {
            return;
        }
        if (!task.getAssignedBy().getId().equals(actor.getId())) {
            throw new ForbiddenActionException("A Director can only delete a task they created themselves.");
        }
    }

    private void requireCanReassign(Person actor, Task task) {
        if (Role.isAtLeastDirector(actor.getRole())) {
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
            throw new ForbiddenActionException("Only a Director, Super Admin, or this team's leader can reassign this task.");
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

    /** Bounds dateAssigned on both sides — mirrors maxAssignableDate/minAssignableDate on
     *  the frontend, which keep the date picker from offering either mistake in the first
     *  place, but this is the authoritative check. Forward: a typo guard only (e.g. picking
     *  2036 instead of 2026). Backward: a real integrity rule, not just a typo guard — real
     *  backfilling (recording a task that actually started last quarter, before anyone got
     *  around to entering it) fits comfortably inside 3 months; anything older reads as a
     *  fat-fingered date and skews "how old is this task" reporting/audit history. */
    private void requireReasonableDate(LocalDate dateAssigned) {
        if (dateAssigned.isAfter(LocalDate.now().plusYears(1))) {
            throw new InvalidAssignmentException("dateAssigned can't be more than a year in the future.");
        }
        if (dateAssigned.isBefore(LocalDate.now().minusMonths(3))) {
            throw new InvalidAssignmentException("dateAssigned can't be more than 3 months in the past.");
        }
    }

    /** A deadline before the task even starts makes no sense — checked once at creation,
     *  same "typo guard" spirit as requireReasonableDate. Never re-checked afterward: the
     *  extension workflow only ever moves a deadline later (see requestDeadlineExtension/
     *  extendDeadlineDirectly), so it can never regress behind dateAssigned once past this. */
    private void requireDeadlineNotBeforeAssignment(LocalDate dateAssigned, LocalDate deadline) {
        if (deadline.isBefore(dateAssigned)) {
            throw new InvalidAssignmentException("deadline can't be before dateAssigned.");
        }
    }

    /** source/sourceLabel are open to anyone creating a task, at any depth. severity is
     *  Executive-only — a Director creating a depth-1 task under a Department root still
     *  can't set it, even when the Department task above it is CRITICAL. CRITICAL sets
     *  pinned = true as a one-time default here, at creation only — pinning itself stays
     *  a separate, independently-editable toggle afterward (see setPinned), never
     *  re-enforced from here. */
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

        // Recorded before the delete, not after — recordActivity reads taskCode/title/
        // assignee straight off the entity, which won't exist to read from once it's gone.
        // Only this one task gets a DELETED entry, not each subtask a cascade takes with
        // it — the log is "what did a Director/Super Admin just do", not a full cascade trace.
        recordActivity(task, TaskActivityAction.DELETED, actor);
        // Same "before it's gone" timing as recordActivity, and the same reasoning —
        // notifyTaskDeleted reads the task's own title/code off the entity too.
        notificationService.notifyTaskDeleted(task, actor);

        // Deleting a top-level task cascades to its subtasks (Task.subtasks is
        // CascadeType.ALL + orphanRemoval). Deleting a subtask needs the parent's rollup
        // recomputed afterward, since its subtask set just shrank.
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

        // A Super Admin's override authority (see requireCanApprove/requireCanReject/
        // isDeadlineOverrideTier) means they CAN decide any request, but neither
        // resolveDeadlineDecider nor resolveDeadlineApprover ever actually resolves TO them
        // — a Super Admin doesn't create tasks in the normal flow, so they'd never naturally
        // show up as anyone's decider/approver and this inbox would always read empty for
        // them even though the authority is real. Show every pending request, org-wide,
        // instead — the one tier that genuinely oversees all of it.
        if (viewer.getRole() == Role.SUPER_ADMIN) {
            return pending.stream()
                    .map(request -> taskMapper.toPendingExtensionResponse(request, true))
                    .toList();
        }

        // The rejecter always sees it immediately. The approver only sees it once it's been
        // explicitly forwarded to them (see forwardExtensionRequestToApprover) — on a
        // CEO-mandated chain that's a distinct person from the rejecter, and the whole point
        // of forwarding is that the CEO's inbox doesn't fill up with every request the
        // moment it's made, only the ones a Director actually decided are worth escalating.
        // On an ordinary Director-originated chain, rejecter and approver are the same
        // person, so this is unaffected — they see their own requests right away either way.
        return pending.stream()
                .filter(request -> {
                    Task task = request.getTask();
                    boolean isDecider = resolveDeadlineDecider(task).getId().equals(viewerId);
                    boolean isApprover = resolveDeadlineApprover(task).getId().equals(viewerId);
                    return isDecider || (isApprover && request.getForwardedAt() != null);
                })
                // canApproveDeadline tells the frontend which action(s) this particular
                // viewer actually has on each row, so a Director who's only the rejecter
                // doesn't get shown an Approve button that would just fail.
                .map(request -> taskMapper.toPendingExtensionResponse(request, canApproveDeadline(viewer, request.getTask())))
                .toList();
    }

    @Override
    public TaskDetailResponse setPinned(Long taskId, SetPinnedRequest request) {
        Task task = taskRepository.findWithDetailsById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        Person changedBy = personRepository.findById(request.changedById())
                .orElseThrow(() -> new ResourceNotFoundException("changedById not found"));
        if (!Role.isAtLeastDirector(changedBy.getRole())) {
            throw new ForbiddenActionException("Only a Director or Super Admin can pin or unpin a task.");
        }

        task.setPinned(request.pinned());
        return taskMapper.toDetailResponse(taskRepository.save(task));
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

    /** Rejecting a request: this task's own deadline decider (see resolveDeadlineDecider),
     *  or the override tier. A "no" changes nothing the mandate above this task depended on,
     *  so — unlike approving, see canApproveDeadline — this is never escalated further even
     *  on a CEO-mandated chain (see isCeoMandated): the Director a request lands on doesn't
     *  need the CEO's sign-off just to decline it. */
    private boolean canRejectDeadline(Person actor, Task task) {
        return isDeadlineOverrideTier(task, actor) || resolveDeadlineDecider(task).getId().equals(actor.getId());
    }

    private void requireCanReject(Person actor, Task task) {
        if (!canRejectDeadline(actor, task)) {
            throw new ForbiddenActionException("Only whoever set this task's deadline, or a Director/Super Admin "
                    + "(Executive/Super Admin for a Department task), can decide a deadline extension.");
        }
    }

    /** Approving a request, or extending directly (an implicit, immediate approval — see
     *  extendDeadlineDirectly): changes the deadline that whatever mandate sits above this
     *  task was built around, so on a CEO-mandated chain (its root task was assigned by an
     *  Executive/Super Admin — see isCeoMandated) only an Executive-or-above may grant it,
     *  no matter how far down the hierarchy this particular task sits or who directly
     *  created it — a Director who made this task an implementation task under the CEO's
     *  own Department task can still reject a request on it (see canRejectDeadline), but
     *  can't be the one to say yes. Everywhere else (an ordinary Director-originated
     *  hierarchy), unchanged: same authority as rejecting. Also backs
     *  PendingExtensionRequestResponse.canApprove, so the "Requests" inbox can hide/disable
     *  the Approve action for a viewer who can only ever reject a given request instead of
     *  showing a button that would just fail. */
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

    /** True when this task's whole hierarchy originates from an Executive/Super Admin's own
     *  mandate — i.e. its root is a Department task the CEO (or Super Admin) assigned
     *  directly, not an ordinary top-level task a plain Director created on their own
     *  initiative. Everything below such a root — the Director's implementation task, and
     *  any leaf subtask under that — inherits the same "only the CEO actually approves"
     *  rule (see requireCanApprove), regardless of who directly created each individual
     *  task along the way. */
    private boolean isCeoMandated(Task task) {
        return Role.isAtLeastExecutive(findRoot(task).getAssignedBy().getRole());
    }

    /** The true approving authority for this task's deadline. Same walk as
     *  resolveDeadlineDecider below, except it doesn't stop at the first Director-tier
     *  assignedBy it finds: on a CEO-mandated chain (see isCeoMandated), the actual approver
     *  is the root's own assignedBy — the CEO (or Super Admin) who set that mandate — even
     *  though a Director further down is who the request first lands on and who resolves as
     *  its rejecter. Identical to resolveDeadlineDecider whenever the chain isn't
     *  CEO-mandated (an ordinary Director-originated hierarchy has only one authority
     *  either way). */
    private Person resolveDeadlineApprover(Task task) {
        Task root = findRoot(task);
        if (Role.isAtLeastExecutive(root.getAssignedBy().getRole())) {
            return root.getAssignedBy();
        }
        return resolveDeadlineDecider(task);
    }

    /** Deadline decisions are a Director's job, not a Team Leader's — a Team Leader can
     *  create a leaf subtask (see createLeafSubtask), which makes them that subtask's own
     *  assignedBy, but they're still just a Member as far as authority over a deadline
     *  goes. So: use this task's own assignedBy if they already hold Director-or-above,
     *  otherwise walk up to its parent (whose creator is always Director-or-above — every
     *  task shape ABOVE a plain leaf subtask is created by a Director, an Executive, or a
     *  Department's head Director, all of which satisfy this) and use that. Chain of
     *  command: whoever's actually doing the work requests, but a real Director decides. */
    private Person resolveDeadlineDecider(Task task) {
        Task current = task;
        while (current != null) {
            if (Role.isAtLeastDirector(current.getAssignedBy().getRole())) {
                return current.getAssignedBy();
            }
            current = current.getParentTask();
        }
        // Unreachable in practice — every hierarchy is rooted in a Director/Executive-
        // created task, so the loop above always returns before running out of ancestors.
        return task.getAssignedBy();
    }
}
