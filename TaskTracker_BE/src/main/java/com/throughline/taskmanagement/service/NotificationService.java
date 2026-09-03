package com.throughline.taskmanagement.service;

import com.throughline.taskmanagement.dto.response.NotificationResponse;
import com.throughline.taskmanagement.enums.Role;
import com.throughline.taskmanagement.model.Person;
import com.throughline.taskmanagement.model.Task;
import com.throughline.taskmanagement.model.TeamMembershipChange;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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

    Page<NotificationResponse> getNotifications(Long recipientId, Pageable pageable);

    /** requesterId must match the notification's recipient — enforced here, not just trusted. */
    NotificationResponse markAsRead(Long notificationId, Long requesterId);

    long getUnreadCount(Long recipientId);
}
