package com.throughline.taskmanagement.service;

import com.throughline.taskmanagement.dto.response.NotificationResponse;
import com.throughline.taskmanagement.model.TeamMembershipChange;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationService {
    /** Called by TeamServiceImpl right after a membership change is persisted — never
     *  invoked directly by a client. Notifies the team's creating Director, unless the
     *  Director is the one who made the change themself. */
    void notifyMembershipChange(TeamMembershipChange change);

    Page<NotificationResponse> getNotifications(Long recipientId, Pageable pageable);

    /** requesterId must match the notification's recipient — enforced here, not just trusted. */
    NotificationResponse markAsRead(Long notificationId, Long requesterId);

    long getUnreadCount(Long recipientId);
}
