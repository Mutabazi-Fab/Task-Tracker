package com.throughline.taskmanagement.service.impl;

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
import com.throughline.taskmanagement.enums.AssigneeType;
import com.throughline.taskmanagement.enums.CreatedByRole;
import com.throughline.taskmanagement.enums.Role;
import com.throughline.taskmanagement.enums.TaskStatus;
import com.throughline.taskmanagement.exception.ForbiddenActionException;
import com.throughline.taskmanagement.exception.InvalidAssignmentException;
import com.throughline.taskmanagement.exception.InvalidProgressException;
import com.throughline.taskmanagement.exception.ResourceNotFoundException;
import com.throughline.taskmanagement.mapper.TaskMapper;
import com.throughline.taskmanagement.model.Person;
import com.throughline.taskmanagement.model.Task;
import com.throughline.taskmanagement.model.TaskComment;
import com.throughline.taskmanagement.model.TaskReassignment;
import com.throughline.taskmanagement.model.Team;
import com.throughline.taskmanagement.model.TeamMember;
import com.throughline.taskmanagement.repository.PersonRepository;
import com.throughline.taskmanagement.repository.TaskCommentRepository;
import com.throughline.taskmanagement.repository.TaskReassignmentRepository;
import com.throughline.taskmanagement.repository.TaskRepository;
import com.throughline.taskmanagement.repository.TeamMemberRepository;
import com.throughline.taskmanagement.repository.TeamRepository;
import com.throughline.taskmanagement.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final PersonRepository personRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TaskCommentRepository taskCommentRepository;
    private final TaskReassignmentRepository taskReassignmentRepository;
    private final TaskMapper taskMapper;

    @Override
    public TaskDetailResponse createTask(CreateTaskRequest request) {
        Person createdBy = personRepository.findById(request.createdById())
                .orElseThrow(() -> new ResourceNotFoundException("createdById not found"));
        if (!Role.isAtLeastDirector(createdBy.getRole())) {
            throw new ForbiddenActionException("Only a Director can create a top-level task.");
        }

        Team assignedTeam = teamRepository.findById(request.assignedTeamId())
                .orElseThrow(() -> new ResourceNotFoundException("assignedTeamId not found"));

        Task task = new Task();
        task.setTaskCode(nextTaskCode());
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setDateAssigned(request.dateAssigned());
        task.setAssignedBy(createdBy);
        task.setAssigneeType(AssigneeType.TEAM);
        task.setAssignedTeam(assignedTeam);
        task.setParentTask(null);
        task.setCreatedByRole(CreatedByRole.DIRECTOR);
        task.setProgressPercentage(0);
        task.setStatus(TaskStatus.PENDING);

        Task savedTask = taskRepository.save(task);
        addOpeningComment(savedTask, createdBy, request.openingNote());

        return taskMapper.toDetailResponse(savedTask);
    }

    @Override
    public TaskDetailResponse createSubtask(Long parentTaskId, CreateSubtaskRequest request) {
        Task parent = taskRepository.findWithDetailsById(parentTaskId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent task not found"));

        if (parent.getParentTask() != null) {
            throw new InvalidAssignmentException("Cannot create a subtask under another subtask — hierarchy is strictly two levels.");
        }
        if (parent.getAssignedTeam() == null) {
            throw new InvalidAssignmentException("Parent task has no assigned team.");
        }
        Long teamId = parent.getAssignedTeam().getId();

        Person createdBy = personRepository.findById(request.createdById())
                .orElseThrow(() -> new ResourceNotFoundException("createdById not found"));

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
        subtask.setAssignedBy(createdBy);
        subtask.setAssigneeType(AssigneeType.INDIVIDUAL);
        subtask.setAssignedPerson(assignedPerson);
        subtask.setParentTask(parent);
        subtask.setCreatedByRole(isDirector ? CreatedByRole.DIRECTOR : CreatedByRole.TEAM_LEADER);
        subtask.setProgressPercentage(0);
        subtask.setStatus(TaskStatus.PENDING);

        Task savedSubtask = taskRepository.save(subtask);
        addOpeningComment(savedSubtask, createdBy, request.openingNote());

        parent.getSubtasks().add(savedSubtask);
        recalculateParentRollup(parent);

        return taskMapper.toDetailResponse(savedSubtask);
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

    /** Recomputes a top-level task's percentage as the average of its subtasks (0 if none),
     *  called whenever a subtask's percentage or existence changes. Never called for a comment
     *  added directly to a top-level task itself — that's narrative only. */
    private void recalculateParentRollup(Task parent) {
        List<Task> subtasks = taskRepository.findByParentTaskId(parent.getId());
        int rollup = subtasks.isEmpty()
                ? 0
                : (int) Math.round(subtasks.stream().mapToInt(Task::getProgressPercentage).average().orElse(0.0));
        parent.setProgressPercentage(rollup);
        recalculateStatus(parent);
        taskRepository.save(parent);
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
    public Page<TaskListResponse> getAllTasks(TaskStatus status, Pageable pageable) {
        Page<Task> tasks;
        if (status != null) {
            tasks = taskRepository.findByStatus(status, pageable);
        } else {
            tasks = taskRepository.findAll(pageable);
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
        // written, so it must not trust the DTO alone.
        if (request.percentageAtComment() < 0 || request.percentageAtComment() > 100) {
            throw new InvalidProgressException("percentageAtComment must be between 0 and 100");
        }

        Person author = personRepository.findById(request.authorId())
                .orElseThrow(() -> new ResourceNotFoundException("Author not found"));

        long commentCount = taskCommentRepository.countByTaskId(taskId);

        TaskComment comment = new TaskComment();
        comment.setTask(task);
        comment.setAuthor(author);
        comment.setPercentageAtComment(request.percentageAtComment());
        comment.setBody(request.body());
        comment.setSequenceNumber((int) commentCount + 1);

        taskCommentRepository.save(comment);
        task.getComments().add(comment);

        if (task.getParentTask() == null) {
            // Top-level task: this comment is a narrative record only. Its percentage is
            // always the subtask average (or 0 with none) — never set by a comment directly.
        } else {
            task.setProgressPercentage(request.percentageAtComment());
            recalculateStatus(task);
            recalculateParentRollup(task.getParentTask());
        }

        return taskMapper.toDetailResponse(taskRepository.save(task));
    }

    @Override
    public TaskDetailResponse reassignTask(Long taskId, ReassignTaskRequest request) {
        Task task = taskRepository.findWithDetailsById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        Person reassignedBy = personRepository.findById(request.reassignedById())
                .orElseThrow(() -> new ResourceNotFoundException("reassignedBy not found"));

        TaskReassignment reassignment = new TaskReassignment();
        reassignment.setTask(task);
        reassignment.setFromAssigneeType(task.getAssigneeType());
        reassignment.setFromPerson(task.getAssignedPerson());
        reassignment.setFromTeam(task.getAssignedTeam());

        if (task.getParentTask() == null) {
            reassignTopLevelTask(task, request, reassignment);
        } else {
            reassignSubtask(task, request, reassignment);
        }

        reassignment.setReassignedBy(reassignedBy);
        reassignment.setReason(request.reason());
        task.getReassignments().add(reassignment);

        return taskMapper.toDetailResponse(taskRepository.save(task));
    }

    /** A top-level task can only move to a different TEAM — never to an individual. */
    private void reassignTopLevelTask(Task task, ReassignTaskRequest request, TaskReassignment reassignment) {
        if (request.newTeamId() == null) {
            throw new InvalidAssignmentException("newTeamId is required to reassign a top-level task.");
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
    public Page<TaskListResponse> searchTasks(String q, Pageable pageable) {
        return taskRepository.search(q, pageable).map(t -> {
            TaskComment lastComment = taskCommentRepository.findFirstByTaskIdOrderByCreatedAtDesc(t.getId()).orElse(null);
            return taskMapper.toListResponse(t, lastComment);
        });
    }

    @Override
    public TaskDetailResponse updateTask(Long id, UpdateTaskRequest request) {
        Task task = taskRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setDateAssigned(request.dateAssigned());

        return taskMapper.toDetailResponse(taskRepository.save(task));
    }

    @Override
    public void deleteTask(Long id) {
        Task task = taskRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        Task parent = task.getParentTask();

        // Deleting a top-level task cascades to its subtasks (Task.subtasks is
        // CascadeType.ALL + orphanRemoval). Deleting a subtask needs the parent's rollup
        // recomputed afterward, since its subtask set just shrank.
        taskRepository.delete(task);

        if (parent != null) {
            recalculateParentRollup(parent);
        }
    }
}
