package com.throughline.taskmanagement.repository;

import com.throughline.taskmanagement.enums.NotificationType;
import com.throughline.taskmanagement.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId, Pageable pageable);

    long countByRecipientIdAndIsReadFalse(Long recipientId);

    // Backs the sidebar's per-section badges (Teams/Departments/Activity) — one query for every type's
    // unread count at once, rather than N separate round-trips.
    @Query("SELECT n.type, COUNT(n) FROM Notification n WHERE n.recipient.id = :recipientId AND n.isRead = false GROUP BY n.type")
    List<Object[]> countUnreadByType(@Param("recipientId") Long recipientId);

    // Backs "I just opened the Teams/Departments/Activity page — clear that badge": every
    // unread notification of these types for this person, so the service layer can mark
    // them all read in one batch instead of one-at-a-time like the bell's individual
    // markAsRead.
    List<Notification> findByRecipientIdAndTypeInAndIsReadFalse(Long recipientId, List<NotificationType> types);

    // Retention — see NotificationCleanupJob. Bulk deletes (one statement each), not
    // load-then-delete, since the whole point is that there may be a lot of rows.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Notification n WHERE n.createdAt < :cutoff")
    int deleteAllCreatedBefore(@Param("cutoff") LocalDateTime cutoff);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Notification n WHERE n.isRead = true AND n.createdAt < :cutoff")
    int deleteReadCreatedBefore(@Param("cutoff") LocalDateTime cutoff);
}
