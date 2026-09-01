package com.throughline.taskmanagement.model;

import com.throughline.taskmanagement.enums.Role;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Append-only audit log of every role change — who was promoted/demoted, to what, by
 * whom, and (optionally) why. Mirrors TeamMembershipChange's audit pattern: never updated,
 * only ever inserted, so the full history survives even after a person's role moves on
 * again.
 */
@Entity
@Table(name = "role_changes", indexes = {
        @Index(name = "idx_role_changes_person_id", columnList = "person_id")
})
@Getter
@Setter
public class RoleChange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    /** Null only if the person had no role at all before (a legacy account being granted
     *  one for the first time). */
    @Enumerated(EnumType.STRING)
    private Role oldRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role newRole;

    /** Always a Super Admin — enforced in PersonServiceImpl, not just trusted here. */
    @ManyToOne(optional = false)
    @JoinColumn(name = "changed_by_id", nullable = false)
    private Person changedBy;

    /** Optional, unlike TeamMembershipChange's reason — a role change doesn't require one,
     *  though a Super Admin can still record why. */
    private String reason;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime timestamp;
}
