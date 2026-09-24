package com.throughline.taskmanagement.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.throughline.taskmanagement.enums.ExtensionRequestStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Append-only, same shape as {@link TaskReassignment} — a row is never deleted or reused. */
@Entity
@Table(name = "task_deadline_extension_requests", indexes = {
        @Index(name = "idx_task_deadline_ext_requests_task_id", columnList = "task_id")
})
@Getter
@Setter
public class TaskDeadlineExtensionRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    @JsonIgnore
    private Task task;

    /** The deadline as it stood the moment this request was made — null only if the task
     *  itself predates the deadline field entirely. */
    @Column
    private LocalDate currentDeadline;

    @NotNull
    @Column(nullable = false)
    private LocalDate requestedDeadline;

    @NotBlank
    @Column(length = 1000, nullable = false)
    private String justification;

    @ManyToOne(optional = false)
    @JoinColumn(name = "requested_by_id", nullable = false)
    private Person requestedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExtensionRequestStatus status = ExtensionRequestStatus.PENDING;

    /** Null while PENDING. Set to whoever approved/rejected it — or, for a direct
     *  extension, the same person as requestedBy. */
    @ManyToOne
    @JoinColumn(name = "decided_by_id")
    private Person decidedBy;

    @Column(length = 1000)
    private String decisionNote;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime requestedAt;

    /** Null while PENDING. */
    private LocalDateTime decidedAt;

    /** Null until explicitly forwarded — see TaskServiceImpl.forwardExtensionRequestToApprover. */
    @ManyToOne
    @JoinColumn(name = "forwarded_by_id")
    private Person forwardedBy;

    private LocalDateTime forwardedAt;
}
