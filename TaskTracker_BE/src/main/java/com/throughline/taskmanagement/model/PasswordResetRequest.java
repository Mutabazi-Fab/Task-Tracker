package com.throughline.taskmanagement.model;

import com.throughline.taskmanagement.enums.PasswordResetRequestStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entirely offline replacement for the old mailed-code forgot-password flow: an
 * unauthenticated person confirms they want a new password, this row records that, every
 * Super Admin gets notified, and a Super Admin resolves it by either setting a new
 * password directly (PersonServiceImpl.setPasswordDirectly, which auto-marks this
 * FULFILLED) or dismissing it outright. Never deleted — same append-only-audit-trail
 * philosophy as RoleChange/AccountStatusChange, just with a mutable status field instead
 * of being purely insert-only, since "resolved" is a real state transition here.
 *
 * At most one PENDING row per person by construction (AuthServiceImpl.
 * createPasswordResetRequest checks for an existing one before inserting another) — this
 * is what stops a repeatedly-clicked "yes, request one" from spamming every Super Admin
 * with duplicate notifications.
 */
@Entity
@Table(name = "password_reset_requests", indexes = {
        @Index(name = "idx_password_reset_requests_person_id", columnList = "person_id")
})
@Getter
@Setter
public class PasswordResetRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PasswordResetRequestStatus status = PasswordResetRequestStatus.PENDING;

    /** The Super Admin who fulfilled or dismissed this — null while PENDING. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by_id")
    private Person resolvedBy;

    private LocalDateTime resolvedAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime requestedAt;
}
