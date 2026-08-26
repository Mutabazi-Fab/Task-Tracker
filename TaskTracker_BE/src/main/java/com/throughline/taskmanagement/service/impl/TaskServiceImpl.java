package com.throughline.taskmanagement.service.impl;

import com.throughline.taskmanagement.dto.request.AddCommentRequest;
import com.throughline.taskmanagement.dto.request.CreateTaskRequest;
import com.throughline.taskmanagement.dto.request.ReassignTaskRequest;
import com.throughline.taskmanagement.dto.request.UpdateTaskRequest;
import com.throughline.taskmanagement.dto.response.TaskDetailResponse;
import com.throughline.taskmanagement.dto.response.TaskListResponse;
import com.throughline.taskmanagement.dto.response.TaskTimelineResponse;
import com.throughline.taskmanagement.enums.AssigneeType;
import com.throughline.taskmanagement.enums.TaskStatus;
import com.throughline.taskmanagement.exception.InvalidAssignmentException;
import com.throughline.taskmanagement.exception.InvalidProgressException;
import com.throughline.taskmanagement.exception.ResourceNotFoundException;
import com.throughline.taskmanagement.mapper.TaskMapper;
import com.throughline.taskmanagement.model.Person;
import com.throughline.taskmanagement.model.Task;
import com.throughline.taskmanagement.model.TaskComment;
import com.throughline.taskmanagement.model.TaskReassignment;
import com.throughline.taskmanagement.model.Team;
import com.throughline.taskmanagement.repository.PersonRepository;
import com.throughline.taskmanagement.repository.TaskCommentRepository;
import com.throughline.taskmanagement.repository.TaskRepository;
import com.throughline.taskmanagement.repository.TeamRepository;
import com.throughline.taskmanagement.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final PersonRepository personRepository;
    private final TeamRepository teamRepository;
    private final TaskCommentRepository taskCommentRepository;
    private final TaskMapper taskMapper;

    @Override
    public TaskDetailResponse createTask(CreateTaskRequest request) {
        validateAssigneeInvariant(request.assigneeType(), request.assignedPersonId(), request.assignedTeamId());

        Task task = new Task();

        int nextSequence = taskRepository.findMaxTaskCodeSequence() + 1;
        task.setTaskCode(String.format("TSK-%04d", nextSequence));

        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setDateAssigned(request.dateAssigned());

        Person assignedBy = personRepository.findById(request.assignedById())
                .orElseThrow(() -> new ResourceNotFoundException("assignedById not found"));
        task.setAssignedBy(assignedBy);
        
        task.setAssigneeType(request.assigneeType());
        if (request.assigneeType() == AssigneeType.INDIVIDUAL) {
            Person assignedPerson = personRepository.findById(request.assignedPersonId())
                    .orElseThrow(() -> new ResourceNotFoundException("assignedPersonId not found"));
            task.setAssignedPerson(assignedPerson);
        } else {
            Team assignedTeam = teamRepository.findById(request.assignedTeamId())
                    .orElseThrow(() -> new ResourceNotFoundException("assignedTeamId not found"));
            task.setAssignedTeam(assignedTeam);
        }

        task.setProgressPercentage(0);
        task.setStatus(TaskStatus.PENDING);

        Task savedTask = taskRepository.save(task);

        TaskComment comment = new TaskComment();
        comment.setTask(savedTask);
        comment.setAuthor(assignedBy);
        comment.setPercentageAtComment(0);
        comment.setBody(request.openingNote());
        comment.setSequenceNumber(1);
        
        taskCommentRepository.save(comment);
        savedTask.getComments().add(comment);

        return taskMapper.toDetailResponse(savedTask);
    }

    private void validateAssigneeInvariant(AssigneeType type, Long personId, Long teamId) {
        if (type == AssigneeType.INDIVIDUAL && (personId == null || teamId != null)) {
            throw new InvalidAssignmentException("For INDIVIDUAL assignment, exactly personId must be provided.");
        }
        if (type == AssigneeType.TEAM && (teamId == null || personId != null)) {
            throw new InvalidAssignmentException("For TEAM assignment, exactly teamId must be provided.");
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
        
        task.setProgressPercentage(request.percentageAtComment());
        recalculateStatus(task);
        task.getComments().add(comment);
        
        return taskMapper.toDetailResponse(taskRepository.save(task));
    }

    @Override
    public TaskDetailResponse reassignTask(Long taskId, ReassignTaskRequest request) {
        Task task = taskRepository.findWithDetailsById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        Person reassignedBy = personRepository.findById(request.reassignedById())
                .orElseThrow(() -> new ResourceNotFoundException("reassignedBy not found"));
                
        validateAssigneeInvariant(request.newAssigneeType(), request.newPersonId(), request.newTeamId());
        
        if (task.getAssigneeType() == request.newAssigneeType()) {
            if (request.newAssigneeType() == AssigneeType.INDIVIDUAL && task.getAssignedPerson().getId().equals(request.newPersonId())) {
                throw new InvalidAssignmentException("Task is already assigned to this person.");
            }
            if (request.newAssigneeType() == AssigneeType.TEAM && task.getAssignedTeam().getId().equals(request.newTeamId())) {
                throw new InvalidAssignmentException("Task is already assigned to this team.");
            }
        }
        
        TaskReassignment reassignment = new TaskReassignment();
        reassignment.setTask(task);
        reassignment.setFromAssigneeType(task.getAssigneeType());
        reassignment.setFromPerson(task.getAssignedPerson());
        reassignment.setFromTeam(task.getAssignedTeam());
        
        reassignment.setToAssigneeType(request.newAssigneeType());
        if (request.newAssigneeType() == AssigneeType.INDIVIDUAL) {
            Person p = personRepository.findById(request.newPersonId())
                    .orElseThrow(() -> new ResourceNotFoundException("newPersonId not found"));
            reassignment.setToPerson(p);
            task.setAssignedPerson(p);
            task.setAssignedTeam(null);
        } else {
            Team t = teamRepository.findById(request.newTeamId())
                    .orElseThrow(() -> new ResourceNotFoundException("newTeamId not found"));
            reassignment.setToTeam(t);
            task.setAssignedTeam(t);
            task.setAssignedPerson(null);
        }
        task.setAssigneeType(request.newAssigneeType());
        
        reassignment.setReassignedBy(reassignedBy);
        reassignment.setReason(request.reason());
        
        task.getReassignments().add(reassignment);
        
        return taskMapper.toDetailResponse(taskRepository.save(task));
    }

    @Override
    public List<TaskTimelineResponse> getTaskProgressTimeline(Long taskId) {
        List<TaskComment> comments = taskCommentRepository.findByTaskIdOrderByCreatedAtAsc(taskId);
        return comments.stream().map(taskMapper::toTimelineResponse).collect(Collectors.toList());
    }

    @Override
    public List<TaskListResponse> searchTasks(String q) {
        return taskRepository.search(q).stream().map(t -> {
            TaskComment lastComment = taskCommentRepository.findFirstByTaskIdOrderByCreatedAtDesc(t.getId()).orElse(null);
            return taskMapper.toListResponse(t, lastComment);
        }).collect(Collectors.toList());
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
        if (!taskRepository.existsById(id)) {
            throw new ResourceNotFoundException("Task not found");
        }
        taskRepository.deleteById(id);
    }
}
