package com.throughline.taskmanagement.repository;

import com.throughline.taskmanagement.enums.NotificationType;
import com.throughline.taskmanagement.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId, Pageable pageable);

    long countByRecipientIdAndIsReadFalse(Long recipientId);

    // Backs the sidebar's per-section badges (Teams/Departments/Activity) — one query for
    // every type's unread count at once, rather than N separate round-trips. Returns only
    // the types that actually have at least one unread row; the service layer fills in 0
    // for everything else so the frontend never has to special-case a missing key.
    @Query("SELECT n.type, COUNT(n) FROM Notification n WHERE n.recipient.id = :recipientId AND n.isRead = false GROUP BY n.type")
    List<Object[]> countUnreadByType(@Param("recipientId") Long recipientId);

    // Backs "I just opened the Teams/Departments/Activity page — clear that badge": every
    // unread notification of these types for this person, so the service layer can mark
    // them all read in one batch instead of one-at-a-time like the bell's individual
    // markAsRead.
    List<Notification> findByRecipientIdAndTypeInAndIsReadFalse(Long recipientId, List<NotificationType> types);
}
