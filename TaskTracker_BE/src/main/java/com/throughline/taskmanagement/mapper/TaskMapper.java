package com.throughline.taskmanagement.mapper;

import com.throughline.taskmanagement.dto.response.*;
import com.throughline.taskmanagement.model.Task;
import com.throughline.taskmanagement.model.TaskComment;
import com.throughline.taskmanagement.model.TaskReassignment;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TaskMapper {

    public CommentResponse toCommentResponse(TaskComment comment) {
        if (comment == null) return null;
        return new CommentResponse(
                comment.getId(),
                comment.getSequenceNumber(),
                comment.getAuthor().getFullName(),
                comment.getPercentageAtComment(),
                comment.getBody(),
                comment.getCreatedAt()
        );
    }

    public ReassignmentResponse toReassignmentResponse(TaskReassignment reassignment) {
        if (reassignment == null) return null;
        String fromName = reassignment.getFromAssigneeType().name().equals("INDIVIDUAL") ?
                (reassignment.getFromPerson() != null ? reassignment.getFromPerson().getFullName() : "Unknown") :
                (reassignment.getFromTeam() != null ? reassignment.getFromTeam().getName() : "Unknown");
        String toName = reassignment.getToAssigneeType().name().equals("INDIVIDUAL") ?
                (reassignment.getToPerson() != null ? reassignment.getToPerson().getFullName() : "Unknown") :
                (reassignment.getToTeam() != null ? reassignment.getToTeam().getName() : "Unknown");

        return new ReassignmentResponse(
                reassignment.getId(),
                fromName,
                toName,
                reassignment.getReassignedBy().getFullName(),
                reassignment.getReason(),
                reassignment.getReassignedAt()
        );
    }

    public TaskTimelineResponse toTimelineResponse(TaskComment comment) {
        if (comment == null) return null;
        return new TaskTimelineResponse(
                comment.getPercentageAtComment(),
                comment.getCreatedAt(),
                comment.getId()
        );
    }

    private String assigneeNameOf(Task task) {
        return task.getAssigneeType().name().equals("INDIVIDUAL") ?
                (task.getAssignedPerson() != null ? task.getAssignedPerson().getFullName() : "Unknown") :
                (task.getAssignedTeam() != null ? task.getAssignedTeam().getName() : "Unknown");
    }

    public SubtaskSummaryResponse toSubtaskSummary(Task subtask) {
        if (subtask == null) return null;
        return new SubtaskSummaryResponse(
                subtask.getId(),
                subtask.getTaskCode(),
                subtask.getTitle(),
                assigneeNameOf(subtask),
                subtask.getStatus(),
                subtask.getProgressPercentage(),
                subtask.getCreatedByRole()
        );
    }

    public TaskListResponse toListResponse(Task task, TaskComment lastComment) {
        if (task == null) return null;
        List<SubtaskSummaryResponse> subtasks = task.getSubtasks() != null ?
                task.getSubtasks().stream().map(this::toSubtaskSummary).toList() : List.of();

        return new TaskListResponse(
                task.getId(),
                task.getTaskCode(),
                task.getTitle(),
                assigneeNameOf(task),
                task.getAssigneeType(),
                task.getStatus(),
                task.getProgressPercentage(),
                task.getDateAssigned(),
                task.getAssignedBy().getFullName(),
                task.getReassignments() != null ? task.getReassignments().size() : 0,
                toCommentResponse(lastComment),
                task.getParentTask() != null ? task.getParentTask().getId() : null,
                task.getCreatedByRole(),
                subtasks
        );
    }

    public TaskDetailResponse toDetailResponse(Task task) {
        if (task == null) return null;

        Long assigneeId = task.getAssigneeType().name().equals("INDIVIDUAL") ?
                (task.getAssignedPerson() != null ? task.getAssignedPerson().getId() : null) :
                (task.getAssignedTeam() != null ? task.getAssignedTeam().getId() : null);

        Long owningTeamId = task.getParentTask() == null
                ? (task.getAssignedTeam() != null ? task.getAssignedTeam().getId() : null)
                : (task.getParentTask().getAssignedTeam() != null ? task.getParentTask().getAssignedTeam().getId() : null);

        List<CommentResponse> comments = task.getComments() != null ?
                task.getComments().stream().map(this::toCommentResponse).toList() : List.of();
        List<ReassignmentResponse> reassignments = task.getReassignments() != null ?
                task.getReassignments().stream().map(this::toReassignmentResponse).toList() : List.of();
        List<TaskTimelineResponse> timeline = task.getComments() != null ?
                task.getComments().stream().map(this::toTimelineResponse).toList() : List.of();
        List<SubtaskSummaryResponse> subtasks = task.getSubtasks() != null ?
                task.getSubtasks().stream().map(this::toSubtaskSummary).toList() : List.of();

        return new TaskDetailResponse(
                task.getId(),
                task.getTaskCode(),
                task.getTitle(),
                task.getDescription(),
                assigneeNameOf(task),
                assigneeId,
                task.getAssigneeType(),
                owningTeamId,
                task.getStatus(),
                task.getProgressPercentage(),
                task.getDateAssigned(),
                task.getAssignedBy().getFullName(),
                task.getAssignedBy().getId(),
                task.getParentTask() != null ? task.getParentTask().getId() : null,
                task.getParentTask() != null ? task.getParentTask().getTaskCode() : null,
                task.getCreatedByRole(),
                subtasks,
                comments,
                reassignments,
                timeline,
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
