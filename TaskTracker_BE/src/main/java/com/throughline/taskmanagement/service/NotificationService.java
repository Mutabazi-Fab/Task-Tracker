package com.throughline.taskmanagement.service;

import com.throughline.taskmanagement.dto.response.NotificationResponse;
import com.throughline.taskmanagement.enums.Role;
import com.throughline.taskmanagement.model.Person;
import com.throughline.taskmanagement.model.Task;
import com.throughline.taskmanagement.model.TaskDeadlineExtensionRequest;
import com.throughline.taskmanagement.model.TaskReassignment;
import com.throughline.taskmanagement.model.TeamMembershipChange;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

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

    /** Called by PersonServiceImpl right after a Super Admin sends someone a password reset
     *  code on their behalf. Notifies the affected person — so if they didn't ask for it
     *  themselves, they'd notice. */
    void notifyPasswordResetRequested(Person person, Person changedBy);

    /** Called by TaskStalenessJob when a task hasn't had a real progress update in a
     *  while. Notifies whoever's actually responsible for it — the assignee for an
     *  individual task/subtask, the team's Leader for a top-level team task. */
    void notifyTaskStalled(Task task, Person recipient, long daysSinceUpdate);

    /** Called by TaskServiceImpl right after a top-level task is created. There's no
     *  single "assignee" for a team-assigned task, so the team's Leader stands in as the
     *  accountable person; an individually-assigned task notifies that person directly.
     *  Never notifies whoever created the task, even if a Team Leader created it for their
     *  own team and happens to lead it (there's no self-assignment case here — creation is
     *  Director/Super-Admin-only). */
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

    /** Called by TaskServiceImpl right after a deadline extension is requested. Notifies
     *  whoever set the task's deadline (its assignedBy) — the person who'll decide it. */
    void notifyDeadlineExtensionRequested(TaskDeadlineExtensionRequest request);

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

    Page<NotificationResponse> getNotifications(Long recipientId, Pageable pageable);

    /** requesterId must match the notification's recipient — enforced here, not just trusted. */
    NotificationResponse markAsRead(Long notificationId, Long requesterId);

    long getUnreadCount(Long recipientId);
}
