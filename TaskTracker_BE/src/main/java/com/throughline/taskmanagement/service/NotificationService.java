package com.throughline.taskmanagement.service;

import com.throughline.taskmanagement.dto.response.NotificationResponse;
import com.throughline.taskmanagement.enums.NotificationType;
import com.throughline.taskmanagement.enums.Role;
import com.throughline.taskmanagement.model.Department;
import com.throughline.taskmanagement.model.PasswordResetRequest;
import com.throughline.taskmanagement.model.Person;
import com.throughline.taskmanagement.model.Task;
import com.throughline.taskmanagement.model.TaskComment;
import com.throughline.taskmanagement.model.TaskDeadlineExtensionRequest;
import com.throughline.taskmanagement.model.TaskReassignment;
import com.throughline.taskmanagement.model.Team;
import com.throughline.taskmanagement.model.TeamMembershipChange;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface NotificationService {
    /** Called by TeamServiceImpl right after a membership change is persisted — never invoked directly by a
     *  client. */
    void notifyMembershipChange(TeamMembershipChange change);

    /** Called by PersonServiceImpl right after a role change is persisted. Notifies the
     *  affected person, not whoever made the change. */
    void notifyRoleChange(Person person, Role oldRole, Role newRole, Person changedBy);

    /** Called by PersonServiceImpl right after an account is (de)activated. */
    void notifyAccountStatusChange(Person person, boolean active, Person changedBy, String reason);

    /** Called by AuthServiceImpl right after an unauthenticated person requests a password reset for their
     *  own account (the "Forgot Password?" flow). */
    void notifyPasswordResetRequestReceived(PasswordResetRequest request);

    /** Called by PersonServiceImpl right after a Super Admin resets someone's TOTP enrollment
     *  (lost/replaced phone). */
    void notifyTotpReset(Person person, Person changedBy);

    /** Called by TaskStalenessJob when a task hasn't had a real progress update in a while. */
    void notifyTaskStalled(Task task, Person recipient, long daysSinceUpdate);

    /** Called right after a top-level task is created. */
    void notifyTaskAssigned(Task task, Person assignedBy);

    /** Called by TaskServiceImpl right after a subtask is created. */
    void notifySubtaskAssigned(Task subtask, Person assignedBy);

    /** Called by TaskServiceImpl right after a top-level task is reassigned. */
    void notifyTaskReassigned(Task task, TaskReassignment reassignment);

    /** Called by TaskServiceImpl right after a subtask is reassigned. */
    void notifySubtaskReassigned(Task subtask, TaskReassignment reassignment);

    /** Called by TaskServiceImpl right after a Department-level task is moved to a different Department. */
    void notifyDepartmentTaskReassigned(Task task, TaskReassignment reassignment);

    /** Called right after a deadline extension is requested. */
    void notifyDeadlineExtensionRequested(TaskDeadlineExtensionRequest request);

    /** Called when a Director forwards a CEO-mandated request to its true approver.
     *  Notifies that approver — their "Requests" inbox only sees it from this point on. */
    void notifyDeadlineExtensionForwarded(TaskDeadlineExtensionRequest request, Person forwardedBy);

    /** Called by TaskServiceImpl right after an extension request is approved. Notifies
     *  whoever requested it. */
    void notifyDeadlineExtensionApproved(TaskDeadlineExtensionRequest request);

    /** Called by TaskServiceImpl right after an extension request is rejected. Notifies
     *  whoever requested it. */
    void notifyDeadlineExtensionRejected(TaskDeadlineExtensionRequest request);

    /** Called by TaskServiceImpl right after a deadline is extended directly (no request/approval
     *  round-trip). */
    void notifyDeadlineExtended(Task task, LocalDate previousDeadline, Person extendedBy);

    /** Called right after a new top-level discussion message (no parentComment) is posted. */
    void notifyDiscussionCommentPosted(TaskComment comment);

    /** Called by TaskServiceImpl right after a reply (parentComment set) is posted.
     *  Notifies whoever wrote the comment being replied to — never the replier themself. */
    void notifyDiscussionReplyPosted(TaskComment reply);

    /** Called by TeamServiceImpl right after a new team is created. */
    void notifyTeamCreated(Team team, Person createdBy);

    /** Called by DepartmentServiceImpl right after a new department is created. */
    void notifyDepartmentCreated(Department department, Person createdBy);

    /** Called by TaskServiceImpl right BEFORE a task is actually deleted (its identity needs to exist to
     *  read from — same ordering as recordActivity's own DELETED entry). */
    void notifyTaskDeleted(Task task, Person deletedBy);

    /** Called by IncidentServiceImpl right after a new incident is reported. Broadcasts to
     *  every Director-or-above except the reporter, same pattern as notifyTeamCreated. */
    void notifyIncidentReported(com.throughline.taskmanagement.model.Incident incident, Person reportedBy);

    /** Called by IncidentServiceImpl whenever an incident's Action Owner is set or changed.
     *  Notifies the new owner directly. Never fires if the owner is unchanged. */
    void notifyIncidentActionOwnerAssigned(com.throughline.taskmanagement.model.Incident incident, Person assignedBy);

    Page<NotificationResponse> getNotifications(Long recipientId, Pageable pageable);

    /** requesterId must match the notification's recipient — enforced here, not just trusted. */
    NotificationResponse markAsRead(Long notificationId, Long requesterId);

    long getUnreadCount(Long recipientId);

    /** Every NotificationType this person has at least one unread notification of, mapped to that count —
     *  backs the sidebar's per-section badges. */
    Map<NotificationType, Long> getUnreadCountsByType(Long recipientId);

    /** Marks every unread notification of any of these types read in one batch — called
     *  when the viewer opens the page a badge points at (Teams/Departments/Activity), so
     *  the badge clears the same way the bell's own per-notification read does, just for a
     *  whole category at once instead of one click per row. requesterId scopes this to the
     *  caller's own notifications, same as markAsRead. */
    void markCategoryRead(Long requesterId, List<NotificationType> types);
}
