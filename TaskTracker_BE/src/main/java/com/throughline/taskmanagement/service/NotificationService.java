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
    /** Called by TeamServiceImpl right after a membership change is persisted — never
     *  invoked directly by a client. Notifies the team's creating Director, unless the
     *  Director is the one who made the change themself. */
    void notifyMembershipChange(TeamMembershipChange change);

    /** Called by PersonServiceImpl right after a role change is persisted. Notifies the
     *  affected person, not whoever made the change. */
    void notifyRoleChange(Person person, Role oldRole, Role newRole, Person changedBy);

    /** Called by PersonServiceImpl right after an account is (de)activated. Notifies the
     *  affected person, including why — this used to be silently dropped even though the
     *  reason is mandatory on the request. */
    void notifyAccountStatusChange(Person person, boolean active, Person changedBy, String reason);

    /** Called by AuthServiceImpl right after an unauthenticated person requests a password
     *  reset for their own account (the "Forgot Password?" flow). Broadcasts to every
     *  Super Admin — only a Super Admin can resolve it (see PersonServiceImpl.
     *  setPasswordDirectly / dismissPasswordResetRequest) — and, unlike the person-facing
     *  side of this flow, does say if the account is currently deactivated: that's exactly
     *  the kind of thing a Super Admin needs to notice (should it be reactivated? or is
     *  this someone probing a disabled account?), whereas telling the *requester* that
     *  would leak account-status information to someone who hasn't logged in yet. */
    void notifyPasswordResetRequestReceived(PasswordResetRequest request);

    /** Called by PersonServiceImpl right after a Super Admin resets someone's TOTP
     *  enrollment (lost/replaced phone). Notifies the affected person, same reasoning as
     *  notifyPasswordResetRequested — so an unrequested reset doesn't go unnoticed. */
    void notifyTotpReset(Person person, Person changedBy);

    /** Called by TaskStalenessJob when a task hasn't had a real progress update in a
     *  while. Notifies whoever's actually responsible for it — the assignee for an
     *  individual task/subtask, the team's Leader for a top-level team task. */
    void notifyTaskStalled(Task task, Person recipient, long daysSinceUpdate);

    /** Called right after a top-level task is created. A team-assigned task notifies its
     *  Leader (there's no single "assignee"); an individually-assigned task notifies that
     *  person directly. Never notifies whoever created the task. */
    void notifyTaskAssigned(Task task, Person assignedBy);

    /** Called by TaskServiceImpl right after a subtask is created. Notifies the assigned
     *  person directly, plus every other member of the team that owns the parent task —
     *  team visibility, so teammates can see who's responsible for what without having to
     *  ask. Never notifies whoever created the subtask. */
    void notifySubtaskAssigned(Task subtask, Person assignedBy);

    /** Called by TaskServiceImpl right after a top-level task is reassigned. Notifies the
     *  new accountable person: the new team's Leader for a team-assigned task, or the new
     *  individual assignee otherwise. */
    void notifyTaskReassigned(Task task, TaskReassignment reassignment);

    /** Called by TaskServiceImpl right after a subtask is reassigned. Notifies the new
     *  assignee directly, plus every other member of the (unchanged) owning team — team
     *  visibility, same reasoning as notifySubtaskAssigned. */
    void notifySubtaskReassigned(Task subtask, TaskReassignment reassignment);

    /** Called by TaskServiceImpl right after a Department-level task is moved to a
     *  different Department. Notifies the new Department's head Director — the old head
     *  isn't notified of losing it, same "only the new owner hears about it" pattern as
     *  notifyTaskReassigned. */
    void notifyDepartmentTaskReassigned(Task task, TaskReassignment reassignment);

    /** Called right after a deadline extension is requested. Notifies this task's deadline
     *  decider — on a CEO-mandated chain, that's the Director it lands on first, not yet
     *  the CEO (see notifyDeadlineExtensionForwarded for when they find out). */
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

    /** Called by TaskServiceImpl right after a deadline is extended directly (no request/
     *  approval round-trip). Notifies the task's accountable person — the same recipient
     *  notifyTaskAssigned/notifyTaskStalled would resolve to. */
    void notifyDeadlineExtended(Task task, LocalDate previousDeadline, Person extendedBy);

    /** Called right after a new top-level discussion message (no parentComment) is posted.
     *  Notifies the owning team when there is one (so a comment on one member's subtask
     *  reaches the whole team), else the individual assignee or the Department's head
     *  Director. Never notifies the comment's own author. */
    void notifyDiscussionCommentPosted(TaskComment comment);

    /** Called by TaskServiceImpl right after a reply (parentComment set) is posted.
     *  Notifies whoever wrote the comment being replied to — never the replier themself. */
    void notifyDiscussionReplyPosted(TaskComment reply);

    /** Called by TeamServiceImpl right after a new team is created. Broadcasts to every
     *  Director-or-above except whoever created it — backs the Teams nav item's "new"
     *  badge (see getUnreadCountsByType). */
    void notifyTeamCreated(Team team, Person createdBy);

    /** Called by DepartmentServiceImpl right after a new department is created. Broadcasts
     *  to every Director-or-above except whoever created it — backs the Departments nav
     *  item's "new" badge. */
    void notifyDepartmentCreated(Department department, Person createdBy);

    /** Called by TaskServiceImpl right BEFORE a task is actually deleted (its identity
     *  needs to exist to read from — same ordering as recordActivity's own DELETED entry).
     *  Broadcasts to every Director-or-above except whoever deleted it — backs the
     *  Activity nav item's "new" badge. */
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

    /** Every NotificationType this person has at least one unread notification of, mapped
     *  to that count — backs the sidebar's per-section badges. A type with zero unread
     *  notifications simply isn't a key in the map; callers default missing keys to 0
     *  rather than this returning every type unconditionally. */
    Map<NotificationType, Long> getUnreadCountsByType(Long recipientId);

    /** Marks every unread notification of any of these types read in one batch — called
     *  when the viewer opens the page a badge points at (Teams/Departments/Activity), so
     *  the badge clears the same way the bell's own per-notification read does, just for a
     *  whole category at once instead of one click per row. requesterId scopes this to the
     *  caller's own notifications, same as markAsRead. */
    void markCategoryRead(Long requesterId, List<NotificationType> types);
}
