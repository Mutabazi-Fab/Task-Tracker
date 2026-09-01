package com.throughline.taskmanagement.service;

import com.throughline.taskmanagement.dto.response.NotificationResponse;
import com.throughline.taskmanagement.enums.Role;
import com.throughline.taskmanagement.model.Person;
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
     *  affected person. */
    void notifyAccountStatusChange(Person person, boolean active, Person changedBy);

    Page<NotificationResponse> getNotifications(Long recipientId, Pageable pageable);

    /** requesterId must match the notification's recipient — enforced here, not just trusted. */
    NotificationResponse markAsRead(Long notificationId, Long requesterId);

    long getUnreadCount(Long recipientId);
}
