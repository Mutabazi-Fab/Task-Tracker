package com.throughline.taskmanagement.model;

import com.throughline.taskmanagement.enums.NotificationType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Generic on purpose: recipient + type + message + a bare relatedEntityId (not a real FK)
 * covers every notification kind, present and future, without a schema change per type.
 * relatedEntityId isn't a foreign key column because different notification types will
 * eventually point at different kinds of entities (a TeamMembershipChange today, maybe a
 * Task tomorrow) — a single typed FK column can't do that, so it's just interpreted
 * according to `type` by whatever reads it.
 */
@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notifications_recipient_id", columnList = "recipient_id"),
        @Index(name = "idx_notifications_recipient_is_read", columnList = "recipient_id, is_read")
})
@Getter
@Setter
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private Person recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @NotBlank
    @Column(length = 1000, nullable = false)
    private String message;

    private Long relatedEntityId;

    @Column(nullable = false)
    private boolean isRead = false;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
