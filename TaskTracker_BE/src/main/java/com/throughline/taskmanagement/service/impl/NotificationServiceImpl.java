package com.throughline.taskmanagement.service.impl;

import com.throughline.taskmanagement.access.IncidentAccessPolicy;
import com.throughline.taskmanagement.dto.response.NotificationResponse;
import com.throughline.taskmanagement.enums.AccessResourceType;
import com.throughline.taskmanagement.enums.AssigneeType;
import com.throughline.taskmanagement.enums.NotificationType;
import com.throughline.taskmanagement.enums.Role;
import com.throughline.taskmanagement.enums.TeamMembershipChangeAction;
import com.throughline.taskmanagement.exception.ForbiddenActionException;
import com.throughline.taskmanagement.exception.ResourceNotFoundException;
import com.throughline.taskmanagement.model.Department;
import com.throughline.taskmanagement.model.Notification;
import com.throughline.taskmanagement.model.PasswordResetRequest;
import com.throughline.taskmanagement.model.Person;
import com.throughline.taskmanagement.model.Task;
import com.throughline.taskmanagement.model.TaskComment;
import com.throughline.taskmanagement.model.TaskDeadlineExtensionRequest;
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

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

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

        NotificationType type;
        String message;
        switch (change.getAction()) {
            case ADDED -> {
                type = NotificationType.TEAM_MEMBER_ADDED;
                message = String.format("%s was added to %s by %s: %s",
                        change.getPerson().getFullName(), team.getName(),
                        change.getChangedBy().getFullName(), change.getReason());
            }
            case REMOVED -> {
                type = NotificationType.TEAM_MEMBER_REMOVED;
                message = String.format("%s was removed from %s by %s: %s",
                        change.getPerson().getFullName(), team.getName(),
                        change.getChangedBy().getFullName(), change.getReason());
            }
            case LEADER_CHANGED -> {
                type = NotificationType.TEAM_LEADER_CHANGED;
                message = String.format("%s was made %s's leader by %s: %s",
                        change.getPerson().getFullName(), team.getName(),
                        change.getChangedBy().getFullName(), change.getReason());
            }
            default -> throw new IllegalStateException("Unhandled TeamMembershipChangeAction: " + change.getAction());
        }

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
    public void notifyPasswordResetRequestReceived(PasswordResetRequest request) {
        Person person = request.getPerson();
        // Deactivated is worth flagging inline, not just discoverable after clicking
        // through — a Super Admin glancing at the bell should immediately wonder "does
        // this need reactivating, or is someone probing a disabled account?" The
        // *requester*-facing side of this flow never reveals this (see
        // AuthServiceImpl.checkEmailForPasswordReset), only the admin-facing side does.
        String message = person.isActive()
                ? String.format("%s requested a new password.", person.getFullName())
                : String.format("%s requested a new password — this account is currently deactivated.",
                        person.getFullName());

        for (Person admin : personRepository.findByRoleIn(List.of(Role.SUPER_ADMIN))) {
            send(admin, NotificationType.PASSWORD_RESET_REQUEST_RECEIVED, message, person.getId());
        }
    }

    @Override
    public void notifyTotpReset(Person person, Person changedBy) {
        String message = String.format(
                "%s reset your two-factor authentication — you'll set it up again with a new QR code next time you log in.",
                changedBy.getFullName());

        Notification notification = new Notification();
        notification.setRecipient(person);
        notification.setType(NotificationType.TOTP_RESET);
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
            // Every member, not just the Leader — the whole team is on the hook for this.
            Team team = task.getAssignedTeam();
            String message = String.format("Your team was assigned a new task \"%s\" by %s.",
                    task.getTitle(), assignedBy.getFullName());
            for (TeamMember member : teamMemberRepository.findByTeamId(team.getId())) {
                Person recipient = member.getPerson();
                if (recipient.getId().equals(assignedBy.getId())) {
                    continue;
                }
                send(recipient, NotificationType.TASK_ASSIGNED, message, task.getId());
            }
        } else if (task.getAssigneeType() == AssigneeType.DEPARTMENT) {
            Person head = task.getAssignedDepartment().getHeadDirector();
            if (head == null || head.getId().equals(assignedBy.getId())) {
                return;
            }
            String message = String.format("Your department was assigned a new task \"%s\" by %s.",
                    task.getTitle(), assignedBy.getFullName());
            send(head, NotificationType.TASK_ASSIGNED, message, task.getId());
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
            // Every member of the new team, not just its Leader — same as notifyTaskAssigned.
            Team newTeam = reassignment.getToTeam();
            String message = String.format("Your team was assigned the task \"%s\" by %s: %s",
                    task.getTitle(), reassignedBy.getFullName(), reassignment.getReason());
            for (TeamMember member : teamMemberRepository.findByTeamId(newTeam.getId())) {
                Person recipient = member.getPerson();
                if (recipient.getId().equals(reassignedBy.getId())) {
                    continue;
                }
                send(recipient, NotificationType.TASK_REASSIGNED, message, task.getId());
            }
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

    @Override
    public void notifyDepartmentTaskReassigned(Task task, TaskReassignment reassignment) {
        Person reassignedBy = reassignment.getReassignedBy();
        Person newHead = reassignment.getToDepartment().getHeadDirector();
        if (newHead == null || newHead.getId().equals(reassignedBy.getId())) {
            return;
        }
        String previousDepartment = reassignment.getFromDepartment() != null
                ? reassignment.getFromDepartment().getName() : "no department";
        String message = String.format("Your department was assigned the task \"%s\" (moved from %s) by %s: %s",
                task.getTitle(), previousDepartment, reassignedBy.getFullName(), reassignment.getReason());
        send(newHead, NotificationType.TASK_REASSIGNED, message, task.getId());
    }

    @Override
    public void notifyDeadlineExtensionRequested(TaskDeadlineExtensionRequest request) {
        Task task = request.getTask();
        Person decider = resolveDeadlineDecider(task);
        Person requestedBy = request.getRequestedBy();
        if (decider.getId().equals(requestedBy.getId())) {
            return;
        }
        String message = String.format("%s requested an extension on \"%s\" (%s) to %s: %s",
                requestedBy.getFullName(), task.getTitle(), task.getTaskCode(),
                request.getRequestedDeadline(), request.getJustification());
        send(decider, NotificationType.DEADLINE_EXTENSION_REQUESTED, message, task.getId());
        // On a CEO-mandated chain, the CEO doesn't hear about this yet — only once the
        // Director explicitly forwards it (see notifyDeadlineExtensionForwarded).
    }

    @Override
    public void notifyDeadlineExtensionForwarded(TaskDeadlineExtensionRequest request, Person forwardedBy) {
        Task task = request.getTask();
        Person approver = resolveDeadlineApprover(task);
        if (approver.getId().equals(forwardedBy.getId())) {
            return;
        }
        String message = String.format("%s sent you an extension request on \"%s\" (%s) to %s: %s",
                forwardedBy.getFullName(), task.getTitle(), task.getTaskCode(),
                request.getRequestedDeadline(), request.getJustification());
        send(approver, NotificationType.DEADLINE_EXTENSION_REQUESTED, message, task.getId());
    }

    /** Mirrors TaskServiceImpl.resolveDeadlineDecider — walks up to the nearest ancestor whose assignedBy
     *  is Director-or-above. */
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

    /** Mirrors TaskServiceImpl.resolveDeadlineApprover — walks up to this task's root; if
     *  its own assignedBy is Executive-or-above (a CEO-mandated chain), that's the true
     *  approver, even though a Director further down is who the request first lands on. */
    private Person resolveDeadlineApprover(Task task) {
        Task root = task;
        while (root.getParentTask() != null) {
            root = root.getParentTask();
        }
        if (Role.isAtLeastExecutive(root.getAssignedBy().getRole())) {
            return root.getAssignedBy();
        }
        return resolveDeadlineDecider(task);
    }

    @Override
    public void notifyDeadlineExtensionApproved(TaskDeadlineExtensionRequest request) {
        Task task = request.getTask();
        Person requestedBy = request.getRequestedBy();
        Person decidedBy = request.getDecidedBy();
        if (decidedBy != null && decidedBy.getId().equals(requestedBy.getId())) {
            return;
        }
        String decider = decidedBy != null ? decidedBy.getFullName() : "someone";
        String message = String.format("Your extension request on \"%s\" (%s) was approved by %s — new deadline %s.",
                task.getTitle(), task.getTaskCode(), decider, request.getRequestedDeadline());
        send(requestedBy, NotificationType.DEADLINE_EXTENSION_APPROVED, message, task.getId());
    }

    @Override
    public void notifyDeadlineExtensionRejected(TaskDeadlineExtensionRequest request) {
        Task task = request.getTask();
        Person requestedBy = request.getRequestedBy();
        Person decidedBy = request.getDecidedBy();
        if (decidedBy != null && decidedBy.getId().equals(requestedBy.getId())) {
            return;
        }
        String decider = decidedBy != null ? decidedBy.getFullName() : "someone";
        String reasonSuffix = request.getDecisionNote() != null && !request.getDecisionNote().isBlank()
                ? ": " + request.getDecisionNote() : ".";
        String message = String.format("Your extension request on \"%s\" (%s) was rejected by %s%s",
                task.getTitle(), task.getTaskCode(), decider, reasonSuffix);
        send(requestedBy, NotificationType.DEADLINE_EXTENSION_REJECTED, message, task.getId());
    }

    @Override
    public void notifyDeadlineExtended(Task task, LocalDate previousDeadline, Person extendedBy) {
        Person recipient = switch (task.getAssigneeType()) {
            case INDIVIDUAL -> task.getAssignedPerson();
            case TEAM -> task.getAssignedTeam() != null
                    ? teamMemberRepository.findByTeamIdAndIsLeaderTrue(task.getAssignedTeam().getId())
                            .map(TeamMember::getPerson).orElse(null)
                    : null;
            case DEPARTMENT -> task.getAssignedDepartment() != null ? task.getAssignedDepartment().getHeadDirector() : null;
        };
        if (recipient == null || recipient.getId().equals(extendedBy.getId())) {
            return;
        }
        String previous = previousDeadline != null ? previousDeadline.toString() : "none";
        String message = String.format("%s extended the deadline on \"%s\" (%s) from %s to %s.",
                extendedBy.getFullName(), task.getTitle(), task.getTaskCode(), previous, task.getDeadline());
        send(recipient, NotificationType.DEADLINE_EXTENDED, message, task.getId());
    }

    @Override
    public void notifyDiscussionCommentPosted(TaskComment comment) {
        Task task = comment.getTask();
        Person author = comment.getAuthor();

        // Same "owning team" resolution as TaskMapper.owningTeamId — so a comment on one
        // member's subtask reaches the whole team, not just that one assignee.
        Team owningTeam = task.getAssigneeType() == AssigneeType.TEAM
                ? task.getAssignedTeam()
                : (task.getParentTask() != null ? task.getParentTask().getAssignedTeam() : null);

        String message = String.format("%s commented on \"%s\" (%s): %s",
                author.getFullName(), task.getTitle(), task.getTaskCode(), comment.getBody());

        if (owningTeam != null) {
            for (TeamMember member : teamMemberRepository.findByTeamId(owningTeam.getId())) {
                Person recipient = member.getPerson();
                if (!recipient.getId().equals(author.getId())) {
                    send(recipient, NotificationType.DISCUSSION_COMMENT_POSTED, message, task.getId());
                }
            }
            return;
        }

        // No owning team — a standalone INDIVIDUAL task/subtask, or a DEPARTMENT task.
        Person recipient = switch (task.getAssigneeType()) {
            case INDIVIDUAL -> task.getAssignedPerson();
            case DEPARTMENT -> task.getAssignedDepartment() != null ? task.getAssignedDepartment().getHeadDirector() : null;
            case TEAM -> null; // unreachable — TEAM always resolves an owningTeam above
        };
        if (recipient != null && !recipient.getId().equals(author.getId())) {
            send(recipient, NotificationType.DISCUSSION_COMMENT_POSTED, message, task.getId());
        }
    }

    @Override
    public void notifyDiscussionReplyPosted(TaskComment reply) {
        TaskComment parent = reply.getParentComment();
        if (parent == null) {
            return; // Defensive — a top-level comment goes through notifyDiscussionCommentPosted instead.
        }
        Person originalAuthor = parent.getAuthor();
        Person replier = reply.getAuthor();
        if (originalAuthor.getId().equals(replier.getId())) {
            return;
        }

        Task task = reply.getTask();
        String message = String.format("%s replied to your comment on \"%s\" (%s): %s",
                replier.getFullName(), task.getTitle(), task.getTaskCode(), reply.getBody());
        send(originalAuthor, NotificationType.DISCUSSION_REPLY_POSTED, message, task.getId());
    }

    @Override
    public void notifyTeamCreated(Team team, Person createdBy) {
        String message = String.format("%s created a new team, \"%s\"%s.",
                createdBy.getFullName(), team.getName(),
                team.getDepartment() != null ? " in " + team.getDepartment().getName() : "");
        broadcastToDirectorsExcept(createdBy, NotificationType.TEAM_CREATED, message, team.getId());
    }

    @Override
    public void notifyDepartmentCreated(Department department, Person createdBy) {
        String message = String.format("%s created a new department, \"%s\".",
                createdBy.getFullName(), department.getName());
        broadcastToDirectorsExcept(createdBy, NotificationType.DEPARTMENT_CREATED, message, department.getId());
    }

    @Override
    public void notifyTaskDeleted(Task task, Person deletedBy) {
        String message = String.format("%s deleted the task \"%s\" (%s).",
                deletedBy.getFullName(), task.getTitle(), task.getTaskCode());
        broadcastToDirectorsExcept(deletedBy, NotificationType.TASK_DELETED, message, task.getId());

        // The person actually doing this work deserves to know too — only for an
        // INDIVIDUAL-assigned task, and skipped when they're Director-or-above (already
        // covered by the broadcast above, which would otherwise reach them twice).
        if (task.getAssigneeType() == AssigneeType.INDIVIDUAL) {
            Person assignee = task.getAssignedPerson();
            boolean isActor = assignee.getId().equals(deletedBy.getId());
            boolean alreadyBroadcast = Role.isAtLeastDirector(assignee.getRole());
            if (!isActor && !alreadyBroadcast) {
                send(assignee, NotificationType.TASK_DELETED, message, task.getId());
            }
        }
    }

    @Override
    public void notifyIncidentReported(com.throughline.taskmanagement.model.Incident incident, Person reportedBy) {
        String message = String.format("%s reported a new incident: \"%s\" (%s).",
                reportedBy.getFullName(), incident.getTitle(), incident.getIncidentCode());
        for (Person recipient : personRepository.findByRoleIn(List.of(Role.DIRECTOR, Role.EXECUTIVE, Role.SUPER_ADMIN))) {
            if (recipient.getId().equals(reportedBy.getId())) {
                continue;
            }
            boolean seesEverything = Role.isAtLeastExecutive(recipient.getRole());
            boolean headOfThatDepartment = recipient.getDepartment() != null
                    && IncidentAccessPolicy.unitMatchesDepartment(incident.getBusinessUnit(), recipient.getDepartment().getName());
            if (seesEverything || headOfThatDepartment) {
                send(recipient, NotificationType.INCIDENT_REPORTED, message, incident.getId());
            }
        }
    }

    @Override
    public void notifyAccessGranted(Person grantee, Person grantedBy, AccessResourceType type, Long resourceId, String resourceLabel) {
        String kind = type == AccessResourceType.INCIDENT ? "incident" : "task";
        String message = String.format("%s shared the %s \"%s\" with you. You can view it and act on it; actions are recorded.",
                grantedBy.getFullName(), kind, resourceLabel);
        NotificationType notificationType = type == AccessResourceType.INCIDENT
                ? NotificationType.INCIDENT_ACCESS_GRANTED : NotificationType.TASK_ACCESS_GRANTED;
        send(grantee, notificationType, message, resourceId);
    }

    @Override
    public void notifyAccessRevoked(Person grantee, Person revokedBy, String resourceLabel) {
        String message = String.format("%s removed your access to \"%s\".", revokedBy.getFullName(), resourceLabel);
        send(grantee, NotificationType.ACCESS_REVOKED, message, null);
    }

    @Override
    public void notifyIncidentActionOwnerAssigned(com.throughline.taskmanagement.model.Incident incident, Person assignedBy) {
        Person owner = incident.getActionOwner();
        if (owner == null || owner.getId().equals(assignedBy.getId())) {
            return;
        }
        String message = String.format("%s assigned you as Action Owner for incident \"%s\" (%s).",
                assignedBy.getFullName(), incident.getTitle(), incident.getIncidentCode());
        send(owner, NotificationType.INCIDENT_ACTION_OWNER_ASSIGNED, message, incident.getId());
    }

    /** Every Director-or-above except whoever did the thing being announced — backs the
     *  three broadcasts above. Deliberately org-wide, not department/team-scoped. */
    private void broadcastToDirectorsExcept(Person exclude, NotificationType type, String message, Long relatedEntityId) {
        for (Person recipient : personRepository.findByRoleIn(List.of(Role.DIRECTOR, Role.EXECUTIVE, Role.SUPER_ADMIN))) {
            if (!recipient.getId().equals(exclude.getId())) {
                send(recipient, type, message, relatedEntityId);
            }
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

    @Override
    public Map<NotificationType, Long> getUnreadCountsByType(Long recipientId) {
        if (!personRepository.existsById(recipientId)) {
            throw new ResourceNotFoundException("Person not found");
        }
        Map<NotificationType, Long> counts = new EnumMap<>(NotificationType.class);
        for (Object[] row : notificationRepository.countUnreadByType(recipientId)) {
            counts.put((NotificationType) row[0], (Long) row[1]);
        }
        return counts;
    }

    @Override
    public void markCategoryRead(Long requesterId, List<NotificationType> types) {
        List<Notification> unread = notificationRepository.findByRecipientIdAndTypeInAndIsReadFalse(requesterId, types);
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
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
