package com.throughline.taskmanagement.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Current membership: one row per (team, person). isLeader is scoped to
 * THIS row, not global — a person can be the leader of one team and a
 * plain member of another via a separate TeamMember row. This is current
 * state only; the permanent history of who was added/removed and why
 * lives in {@link TeamMembershipChange}, not here.
 */
@Entity
@Table(name = "team_members",
        uniqueConstraints = @UniqueConstraint(columnNames = {"team_id", "person_id"}),
        // The unique constraint above already indexes (team_id, person_id) together — which
        // covers lookups by team_id alone (leftmost prefix), but NOT lookups by person_id
        // alone (findByPersonId), so that needs its own index.
        indexes = @Index(name = "idx_team_members_person_id", columnList = "person_id"))
@Getter
@Setter
public class TeamMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(optional = false)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    @Column(nullable = false)
    private boolean isLeader = false;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime joinedAt;
}
