package com.throughline.taskmanagement.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Append-only audit log of every account activation/deactivation — who was locked out or
 * restored, by whom, when, and why. Mirrors RoleChange's audit pattern exactly (never
 * updated, only ever inserted) — this used to have no persisted trail at all, just a
 * reason-less notification to the affected person that nobody else could ever look back at.
 */
@Entity
@Table(name = "account_status_changes", indexes = {
        @Index(name = "idx_account_status_changes_person_id", columnList = "person_id")
})
@Getter
@Setter
public class AccountStatusChange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    /** The status this change moved the account TO — true = reactivated, false = deactivated. */
    @Column(nullable = false)
    private boolean active;

    /** Always a Super Admin — enforced in PersonServiceImpl, not just trusted here. */
    @ManyToOne(optional = false)
    @JoinColumn(name = "changed_by_id", nullable = false)
    private Person changedBy;

    /** Mandatory — SetAccountActiveRequest.reason is @NotBlank, and PersonAdminControls
     *  keeps the button disabled without one, same rule as everywhere else in this app. */
    @Column(nullable = false)
    private String reason;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime timestamp;
}
