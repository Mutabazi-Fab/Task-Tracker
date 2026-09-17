package com.throughline.taskmanagement.mapper;

import com.throughline.taskmanagement.dto.response.*;
import com.throughline.taskmanagement.enums.AssigneeType;
import com.throughline.taskmanagement.enums.CommentType;
import com.throughline.taskmanagement.enums.Role;
import com.throughline.taskmanagement.model.Department;
import com.throughline.taskmanagement.model.Person;
import com.throughline.taskmanagement.model.Task;
import com.throughline.taskmanagement.model.TaskComment;
import com.throughline.taskmanagement.model.TaskDeadlineExtensionRequest;
import com.throughline.taskmanagement.model.TaskReassignment;
import com.throughline.taskmanagement.model.Team;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

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
                comment.getType(),
                comment.getParentComment() != null ? comment.getParentComment().getId() : null,
                comment.getCreatedAt()
        );
    }

    private String reassignmentPartyName(AssigneeType type, Person person, Team team, Department department) {
        if (type == null) return "Unknown";
        return switch (type) {
            case INDIVIDUAL -> person != null ? person.getFullName() : "Unknown";
            case TEAM -> team != null ? team.getName() : "Unknown";
            case DEPARTMENT -> department != null ? department.getName() : "Unknown";
        };
    }

    public ReassignmentResponse toReassignmentResponse(TaskReassignment reassignment) {
        if (reassignment == null) return null;
        String fromName = reassignmentPartyName(reassignment.getFromAssigneeType(),
                reassignment.getFromPerson(), reassignment.getFromTeam(), reassignment.getFromDepartment());
        String toName = reassignmentPartyName(reassignment.getToAssigneeType(),
                reassignment.getToPerson(), reassignment.getToTeam(), reassignment.getToDepartment());

        return new ReassignmentResponse(
                reassignment.getId(),
                fromName,
                toName,
                reassignment.getReassignedBy().getFullName(),
                reassignment.getReason(),
                reassignment.getReassignedAt()
        );
    }

    public DeadlineExtensionResponse toDeadlineExtensionResponse(TaskDeadlineExtensionRequest request) {
        if (request == null) return null;
        return new DeadlineExtensionResponse(
                request.getId(),
                request.getCurrentDeadline(),
                request.getRequestedDeadline(),
                request.getJustification(),
                request.getRequestedBy().getFullName(),
                request.getStatus(),
                request.getDecidedBy() != null ? request.getDecidedBy().getFullName() : null,
                request.getDecisionNote(),
                request.getRequestedAt(),
                request.getDecidedAt()
        );
    }

    /** Same source row as toDeadlineExtensionResponse, but carrying the task's own
     *  identity — the "Requests" inbox is aggregated across tasks, so the viewer needs to
     *  know which task each row belongs to; a single task's own history panel already knows
     *  that from context and doesn't. */
    public PendingExtensionRequestResponse toPendingExtensionResponse(TaskDeadlineExtensionRequest request, boolean canApprove) {
        if (request == null) return null;
        Task task = request.getTask();
        return new PendingExtensionRequestResponse(
                request.getId(),
                task.getId(),
                task.getTaskCode(),
                task.getTitle(),
                request.getCurrentDeadline(),
                request.getRequestedDeadline(),
                request.getJustification(),
                request.getRequestedBy().getFullName(),
                request.getRequestedAt(),
                canApprove,
                request.getForwardedAt() != null
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

    /** A rollup task (TEAM/DEPARTMENT) never carries its own PROGRESS comments — its
     *  percentage is purely derived from its children (see TaskServiceImpl.
     *  recalculateParentRollup) — so without this, its Trend chart on the task detail page
     *  is always empty, even though its rollup value genuinely does move over time as those
     *  children log progress. Reconstructs that history by replaying every descendant leaf's
     *  own comment timeline in chronological order, recomputing this task's rollup value at
     *  each event the exact same way recalculateParentRollup computes it live — average of
     *  the direct children's value — just evaluated at every historical instant instead of
     *  only "now" (the reconstruction's value at the latest instant always matches the
     *  task's actual stored progressPercentage, by construction).
     *
     *  An individually-logged task (its own PROGRESS comments non-empty) is the base case —
     *  its real history, unchanged from before. A task with neither its own comments nor any
     *  children (a fresh task nobody's touched yet) yields an empty timeline, same as always. */
    private List<TaskTimelineResponse> buildRollupTimeline(Task task) {
        List<TaskTimelineResponse> ownProgress = task.getComments() == null ? List.of()
                : task.getComments().stream()
                        .filter(c -> c.getType() == CommentType.PROGRESS)
                        .sorted(Comparator.comparing(TaskComment::getCreatedAt))
                        .map(this::toTimelineResponse)
                        .toList();
        if (!ownProgress.isEmpty()) {
            return ownProgress;
        }

        List<Task> children = task.getSubtasks() == null ? List.of() : task.getSubtasks();
        if (children.isEmpty()) {
            return List.of();
        }

        List<List<TaskTimelineResponse>> childTimelines = children.stream()
                .map(this::buildRollupTimeline)
                .toList();
        if (childTimelines.stream().allMatch(List::isEmpty)) {
            return List.of();
        }

        // Every distinct instant any child (or descendant) logged something, in order.
        TreeSet<LocalDateTime> instants = new TreeSet<>();
        childTimelines.forEach(timeline -> timeline.forEach(point -> instants.add(point.date())));

        // A running cursor per child into its own (already chronological) timeline.
        int[] cursors = new int[children.size()];
        List<TaskTimelineResponse> result = new ArrayList<>();
        for (LocalDateTime instant : instants) {
            double sum = 0;
            for (int i = 0; i < children.size(); i++) {
                List<TaskTimelineResponse> childTimeline = childTimelines.get(i);
                while (cursors[i] < childTimeline.size() && !childTimeline.get(cursors[i]).date().isAfter(instant)) {
                    cursors[i]++;
                }
                // This child's value as of `instant`: its latest point at or before it, or 0
                // if it hadn't logged anything yet by then.
                sum += cursors[i] == 0 ? 0 : childTimeline.get(cursors[i] - 1).percentage();
            }
            result.add(new TaskTimelineResponse((int) Math.round(sum / children.size()), instant, null));
        }
        return result;
    }

    private String assigneeNameOf(Task task) {
        return switch (task.getAssigneeType()) {
            case INDIVIDUAL -> task.getAssignedPerson() != null ? task.getAssignedPerson().getFullName() : "Unknown";
            case TEAM -> task.getAssignedTeam() != null ? task.getAssignedTeam().getName() : "Unknown";
            case DEPARTMENT -> task.getAssignedDepartment() != null ? task.getAssignedDepartment().getName() : "Unknown";
        };
    }

    private Long assigneeIdOf(Task task) {
        return switch (task.getAssigneeType()) {
            case INDIVIDUAL -> task.getAssignedPerson() != null ? task.getAssignedPerson().getId() : null;
            case TEAM -> task.getAssignedTeam() != null ? task.getAssignedTeam().getId() : null;
            case DEPARTMENT -> task.getAssignedDepartment() != null ? task.getAssignedDepartment().getId() : null;
        };
    }

    public SubtaskSummaryResponse toSubtaskSummary(Task subtask) {
        if (subtask == null) return null;
        return new SubtaskSummaryResponse(
                subtask.getId(),
                subtask.getTaskCode(),
                subtask.getTitle(),
                assigneeNameOf(subtask),
                subtask.getAssigneeType(),
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
                task.getDeadline(),
                task.getSource(),
                task.getSourceLabel(),
                task.getSeverity(),
                task.isPinned(),
                task.getAssignedBy().getFullName(),
                task.getReassignments() != null ? task.getReassignments().size() : 0,
                toCommentResponse(lastComment),
                task.getParentTask() != null ? task.getParentTask().getId() : null,
                task.getParentTask() != null ? task.getParentTask().getTaskCode() : null,
                task.getCreatedByRole(),
                subtasks,
                task.getDepth(),
                task.getCreatedAt()
        );
    }

    /** Same chain-of-command resolution as TaskServiceImpl.resolveDeadlineDecider /
     *  NotificationServiceImpl's copy of it: deadline decisions are a Director's job, even
     *  when this task's own assignedBy is a mere Team Leader (who can create a leaf
     *  subtask — see TaskServiceImpl.createLeafSubtask — but has no authority over its
     *  deadline). Walk up to the nearest ancestor whose assignedBy is already Director-or-
     *  above. */
    private Person resolveDeadlineDecider(Task task) {
        Task current = task;
        while (current != null) {
            if (Role.isAtLeastDirector(current.getAssignedBy().getRole())) {
                return current.getAssignedBy();
            }
            current = current.getParentTask();
        }
        return task.getAssignedBy();
    }

    /** Same department-derivation rule as TaskServiceImpl.resolveTaskDepartment: assignedDepartment
     *  directly for a DEPARTMENT-type task, the team's own department for a TEAM-type one,
     *  the assignee's own department for an INDIVIDUAL one. */
    private Long resolveTaskDepartmentId(Task task) {
        return switch (task.getAssigneeType()) {
            case DEPARTMENT -> task.getAssignedDepartment() != null ? task.getAssignedDepartment().getId() : null;
            case TEAM -> task.getAssignedTeam() != null && task.getAssignedTeam().getDepartment() != null
                    ? task.getAssignedTeam().getDepartment().getId() : null;
            case INDIVIDUAL -> task.getAssignedPerson() != null && task.getAssignedPerson().getDepartment() != null
                    ? task.getAssignedPerson().getDepartment().getId() : null;
        };
    }

    public TaskDetailResponse toDetailResponse(Task task) {
        if (task == null) return null;

        Long assigneeId = assigneeIdOf(task);
        Person deadlineDecider = resolveDeadlineDecider(task);

        // "The team that currently owns this task": the task's OWN team when it's TEAM-
        // assigned (a real top-level task, or a depth-1 Department implementation task);
        // otherwise its parent's team, but only when that parent is itself TEAM-assigned
        // (an ordinary leaf subtask) — a DEPARTMENT- or INDIVIDUAL-assigned task has no
        // "owning team" of its own to inherit. Checking the task's OWN assigneeType first,
        // rather than "parentTask == null", is what makes this correct one level deeper.
        Long owningTeamId = task.getAssigneeType() == AssigneeType.TEAM
                ? (task.getAssignedTeam() != null ? task.getAssignedTeam().getId() : null)
                : (task.getParentTask() != null && task.getParentTask().getAssignedTeam() != null
                        ? task.getParentTask().getAssignedTeam().getId() : null);

        List<CommentResponse> comments = task.getComments() != null ?
                task.getComments().stream().map(this::toCommentResponse).toList() : List.of();
        List<ReassignmentResponse> reassignments = task.getReassignments() != null ?
                task.getReassignments().stream().map(this::toReassignmentResponse).toList() : List.of();
        // DISCUSSION comments never carry a real percentage reading (see CommentType) —
        // excluded here so the trend chart only ever plots genuine progress updates. A
        // rollup task (TEAM/DEPARTMENT) has none of its own, so its history is reconstructed
        // from its children instead — see buildRollupTimeline.
        List<TaskTimelineResponse> timeline = buildRollupTimeline(task);
        List<SubtaskSummaryResponse> subtasks = task.getSubtasks() != null ?
                task.getSubtasks().stream().map(this::toSubtaskSummary).toList() : List.of();
        List<DeadlineExtensionResponse> deadlineExtensions = task.getDeadlineExtensionRequests() != null ?
                task.getDeadlineExtensionRequests().stream().map(this::toDeadlineExtensionResponse).toList() : List.of();

        return new TaskDetailResponse(
                task.getId(),
                task.getTaskCode(),
                task.getTitle(),
                task.getDescription(),
                assigneeNameOf(task),
                assigneeId,
                task.getAssigneeType(),
                resolveTaskDepartmentId(task),
                owningTeamId,
                task.getStatus(),
                task.getProgressPercentage(),
                task.getDateAssigned(),
                task.getDeadline(),
                task.getSource(),
                task.getSourceLabel(),
                task.getSeverity(),
                task.isPinned(),
                task.getAssignedBy().getFullName(),
                task.getAssignedBy().getId(),
                task.getAssignedBy().getRole(),
                deadlineDecider.getFullName(),
                deadlineDecider.getId(),
                task.getParentTask() != null ? task.getParentTask().getId() : null,
                task.getParentTask() != null ? task.getParentTask().getTaskCode() : null,
                task.getCreatedByRole(),
                task.getDepth(),
                subtasks,
                comments,
                reassignments,
                deadlineExtensions,
                timeline,
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
