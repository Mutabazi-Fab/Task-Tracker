package com.throughline.taskmanagement.model;

import com.throughline.taskmanagement.enums.TeamMembershipChangeAction;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/** Append-only audit log — never updated, never deleted. */
@Entity
@Table(name = "team_membership_changes", indexes = {
        @Index(name = "idx_team_membership_changes_team_id", columnList = "team_id"),
        @Index(name = "idx_team_membership_changes_timestamp", columnList = "timestamp")
})
@Getter
@Setter
public class TeamMembershipChange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    /** The member this change is about. */
    @ManyToOne(optional = false)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TeamMembershipChangeAction action;

    /** Who performed the add/remove — the Team Leader, or a Director. */
    @ManyToOne(optional = false)
    @JoinColumn(name = "changed_by_id", nullable = false)
    private Person changedBy;

    @NotBlank
    @Column(length = 1000, nullable = false)
    private String reason;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime timestamp;
}
