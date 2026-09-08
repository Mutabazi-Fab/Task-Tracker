package com.throughline.taskmanagement.service.impl;

import com.throughline.taskmanagement.dto.response.NotificationResponse;
import com.throughline.taskmanagement.enums.AssigneeType;
import com.throughline.taskmanagement.enums.NotificationType;
import com.throughline.taskmanagement.enums.Role;
import com.throughline.taskmanagement.enums.TeamMembershipChangeAction;
import com.throughline.taskmanagement.exception.ForbiddenActionException;
import com.throughline.taskmanagement.exception.ResourceNotFoundException;
import com.throughline.taskmanagement.model.Notification;
import com.throughline.taskmanagement.model.Person;
import com.throughline.taskmanagement.model.Task;
import com.throughline.taskmanagement.model.TaskReassignment;
import com.throughline.taskmanagement.model.Team;
import com.throughline.taskmanagement.model.TeamMember;
import com.throughline.taskmanagement.model.TeamMembershipChange;
import com.throughline.taskmanagement.repository.NotificationRepository;
import com.throughline.taskmanagement.repository.PersonRepository;
import com.throughline.taskmanagement.repository.TeamMemberRepository;
import com.throughline.taskmanagement.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final PersonRepository personRepository;
    private final TeamMemberRepository teamMemberRepository;

    @Override
    public void notifyMembershipChange(TeamMembershipChange change) {
        Team team = change.getTeam();
        Person director = team.getCreatedBy();

        if (director == null) {
            // Team predates the createdBy field (e.g. seeded before Phase 2) — nobody to notify.
            return;
        }
        if (director.getId().equals(change.getChangedBy().getId())) {
            // The Director made this change themself — no point notifying them of their own action.
            return;
        }

        NotificationType type = change.getAction() == TeamMembershipChangeAction.ADDED
                ? NotificationType.TEAM_MEMBER_ADDED
                : NotificationType.TEAM_MEMBER_REMOVED;
        String actionWord = change.getAction() == TeamMembershipChangeAction.ADDED ? "added to" : "removed from";
        String message = String.format("%s was %s %s by %s: %s",
                change.getPerson().getFullName(), actionWord, team.getName(),
                change.getChangedBy().getFullName(), change.getReason());

        Notification notification = new Notification();
        notification.setRecipient(director);
        notification.setType(type);
        notification.setMessage(message);
        notification.setRelatedEntityId(change.getId());
        notificationRepository.save(notification);
    }

    @Override
    public void notifyRoleChange(Person person, Role oldRole, Role newRole, Person changedBy) {
        String message = String.format("Your role was changed from %s to %s by %s.",
                oldRole == null ? "none" : oldRole.name(), newRole.name(), changedBy.getFullName());

        Notification notification = new Notification();
        notification.setRecipient(person);
        notification.setType(NotificationType.ROLE_CHANGED);
        notification.setMessage(message);
        notification.setRelatedEntityId(person.getId());
        notificationRepository.save(notification);
    }

    @Override
    public void notifyAccountStatusChange(Person person, boolean active, Person changedBy, String reason) {
        String message = active
                ? String.format("Your account was reactivated by %s: %s", changedBy.getFullName(), reason)
                : String.format("Your account was deactivated by %s: %s", changedBy.getFullName(), reason);

        Notification notification = new Notification();
        notification.setRecipient(person);
        notification.setType(NotificationType.ACCOUNT_STATUS_CHANGED);
        notification.setMessage(message);
        notification.setRelatedEntityId(person.getId());
        notificationRepository.save(notification);
    }

    @Override
    public void notifyPasswordResetRequested(Person person, Person changedBy) {
        String message = String.format(
                "%s sent you a password reset code — check your email to set a new password.",
                changedBy.getFullName());

        Notification notification = new Notification();
        notification.setRecipient(person);
        notification.setType(NotificationType.PASSWORD_RESET_REQUESTED);
        notification.setMessage(message);
        notification.setRelatedEntityId(person.getId());
        notificationRepository.save(notification);
    }

    @Override
    public void notifyTaskStalled(Task task, Person recipient, long daysSinceUpdate) {
        String message = String.format("%s (%s) hasn't moved in %d day%s — still at %d%%.",
                task.getTaskCode(), task.getTitle(), daysSinceUpdate, daysSinceUpdate == 1 ? "" : "s",
                task.getProgressPercentage());

        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setType(NotificationType.TASK_STALLED);
        notification.setMessage(message);
        notification.setRelatedEntityId(task.getId());
        notificationRepository.save(notification);
    }

    @Override
    public void notifyTaskAssigned(Task task, Person assignedBy) {
        if (task.getAssigneeType() == AssigneeType.TEAM) {
            Team team = task.getAssignedTeam();
            teamMemberRepository.findByTeamIdAndIsLeaderTrue(team.getId()).ifPresent(leadership -> {
                Person leader = leadership.getPerson();
                if (leader.getId().equals(assignedBy.getId())) {
                    return;
                }
                String message = String.format("Your team was assigned a new task \"%s\" by %s.",
                        task.getTitle(), assignedBy.getFullName());
                send(leader, NotificationType.TASK_ASSIGNED, message, task.getId());
            });
        } else {
            Person assignee = task.getAssignedPerson();
            if (assignee.getId().equals(assignedBy.getId())) {
                return;
            }
            String message = String.format("You were assigned a new task \"%s\" by %s.",
                    task.getTitle(), assignedBy.getFullName());
            send(assignee, NotificationType.TASK_ASSIGNED, message, task.getId());
        }
    }

    @Override
    public void notifySubtaskAssigned(Task subtask, Person assignedBy) {
        Task parent = subtask.getParentTask();
        Person assignee = subtask.getAssignedPerson();
        Team team = parent.getAssignedTeam();

        if (!assignee.getId().equals(assignedBy.getId())) {
            String directMessage = String.format("You were assigned a new subtask \"%s\" under \"%s\" by %s.",
                    subtask.getTitle(), parent.getTitle(), assignedBy.getFullName());
            send(assignee, NotificationType.SUBTASK_ASSIGNED, directMessage, subtask.getId());
        }

        String broadcastMessage = String.format("%s was assigned the subtask \"%s\" under \"%s\" by %s.",
                assignee.getFullName(), subtask.getTitle(), parent.getTitle(), assignedBy.getFullName());
        for (TeamMember member : teamMemberRepository.findByTeamId(team.getId())) {
            Person recipient = member.getPerson();
            boolean alreadyNotifiedDirectly = recipient.getId().equals(assignee.getId());
            boolean isActor = recipient.getId().equals(assignedBy.getId());
            if (alreadyNotifiedDirectly || isActor) {
                continue;
            }
            send(recipient, NotificationType.SUBTASK_ASSIGNED, broadcastMessage, subtask.getId());
        }
    }

    @Override
    public void notifyTaskReassigned(Task task, TaskReassignment reassignment) {
        Person reassignedBy = reassignment.getReassignedBy();

        if (reassignment.getToAssigneeType() == AssigneeType.TEAM) {
            Team newTeam = reassignment.getToTeam();
            teamMemberRepository.findByTeamIdAndIsLeaderTrue(newTeam.getId()).ifPresent(leadership -> {
                Person leader = leadership.getPerson();
                if (leader.getId().equals(reassignedBy.getId())) {
                    return;
                }
                String message = String.format("Your team was assigned the task \"%s\" by %s: %s",
                        task.getTitle(), reassignedBy.getFullName(), reassignment.getReason());
                send(leader, NotificationType.TASK_REASSIGNED, message, task.getId());
            });
        } else {
            Person newAssignee = reassignment.getToPerson();
            if (newAssignee.getId().equals(reassignedBy.getId())) {
                return;
            }
            String previousOwner = reassignment.getFromPerson() != null
                    ? reassignment.getFromPerson().getFullName() : "nobody";
            String message = String.format("You were reassigned the task \"%s\" from %s by %s: %s",
                    task.getTitle(), previousOwner, reassignedBy.getFullName(), reassignment.getReason());
            send(newAssignee, NotificationType.TASK_REASSIGNED, message, task.getId());
        }
    }

    @Override
    public void notifySubtaskReassigned(Task subtask, TaskReassignment reassignment) {
        Task parent = subtask.getParentTask();
        Team team = parent.getAssignedTeam();
        Person reassignedBy = reassignment.getReassignedBy();
        Person newAssignee = reassignment.getToPerson();
        String previousOwner = reassignment.getFromPerson() != null
                ? reassignment.getFromPerson().getFullName() : "nobody";

        if (!newAssignee.getId().equals(reassignedBy.getId())) {
            String directMessage = String.format("You were reassigned the subtask \"%s\" under \"%s\" from %s by %s: %s",
                    subtask.getTitle(), parent.getTitle(), previousOwner, reassignedBy.getFullName(), reassignment.getReason());
            send(newAssignee, NotificationType.SUBTASK_REASSIGNED, directMessage, subtask.getId());
        }

        String broadcastMessage = String.format("The subtask \"%s\" under \"%s\" was reassigned from %s to %s by %s: %s",
                subtask.getTitle(), parent.getTitle(), previousOwner, newAssignee.getFullName(),
                reassignedBy.getFullName(), reassignment.getReason());
        for (TeamMember member : teamMemberRepository.findByTeamId(team.getId())) {
            Person recipient = member.getPerson();
            boolean alreadyNotifiedDirectly = recipient.getId().equals(newAssignee.getId());
            boolean isActor = recipient.getId().equals(reassignedBy.getId());
            if (alreadyNotifiedDirectly || isActor) {
                continue;
            }
            send(recipient, NotificationType.SUBTASK_REASSIGNED, broadcastMessage, subtask.getId());
        }
    }

    private void send(Person recipient, NotificationType type, String message, Long relatedEntityId) {
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setType(type);
        notification.setMessage(message);
        notification.setRelatedEntityId(relatedEntityId);
        notificationRepository.save(notification);
    }

    @Override
    public Page<NotificationResponse> getNotifications(Long recipientId, Pageable pageable) {
        if (!personRepository.existsById(recipientId)) {
            throw new ResourceNotFoundException("Person not found");
        }
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId, pageable)
                .map(this::toResponse);
    }

    @Override
    public NotificationResponse markAsRead(Long notificationId, Long requesterId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        if (!notification.getRecipient().getId().equals(requesterId)) {
            throw new ForbiddenActionException("You can only mark your own notifications as read.");
        }

        notification.setRead(true);
        return toResponse(notificationRepository.save(notification));
    }

    @Override
    public long getUnreadCount(Long recipientId) {
        if (!personRepository.existsById(recipientId)) {
            throw new ResourceNotFoundException("Person not found");
        }
        return notificationRepository.countByRecipientIdAndIsReadFalse(recipientId);
    }

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getType(),
                n.getMessage(),
                n.getRelatedEntityId(),
                n.isRead(),
                n.getCreatedAt()
        );
    }
}
