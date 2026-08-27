package com.throughline.taskmanagement.model;

import com.throughline.taskmanagement.enums.Role;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/** No `team` field here — a person can belong to multiple teams simultaneously,
 *  so membership is modeled as {@link TeamMember} join rows, not a single FK. */
@Entity
@Table(name = "persons")
@Getter
@Setter
public class Person {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String fullName;

    @Email
    @NotBlank
    @Column(unique = true, nullable = false)
    private String email;

    /** Job title, e.g. "Backend Engineer" — NOT the authorization role. See {@link #role}. */
    @Column(nullable = false)
    private String jobTitle;

    /**
     * Military rank (e.g. "Major", "Captain", "Lieutenant Colonel") — optional.
     * Set only for officers; left null for civilian staff. No validation
     * annotation on purpose: this is never a required field.
     */
    private String rank;

    /**
     * Authorization role (DIRECTOR / MEMBER). Nullable on purpose: existing
     * rows created before auth existed have no role yet, and are treated as
     * MEMBER wherever this is checked rather than requiring a migration.
     */
    @Enumerated(EnumType.STRING)
    private Role role;

    /**
     * BCrypt hash, nullable. A null password means this account has never
     * been provisioned to log in (e.g. seeded before auth existed) — such
     * accounts exist in the system but simply can't authenticate until they
     * sign up with this same email, which claims the record instead of
     * creating a duplicate.
     */
    private String password;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
