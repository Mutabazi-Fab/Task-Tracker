package com.throughline.taskmanagement.model;

import com.throughline.taskmanagement.enums.Role;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
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

    /** Every person belongs to exactly one Department, independent of team membership —
     *  someone assigned work directly as an individual, with no team at all, still visibly
     *  belongs to a department. Required in practice (PersonServiceImpl.createPerson), not
     *  a DB constraint. Nullable only for accounts that predate this field. */
    @ManyToOne
    @JoinColumn(name = "department_id")
    private Department department;

    /**
     * Stored in plaintext (see SecurityConfig's NoOpPasswordEncoder — a deliberate,
     * explicit choice for this project). Set directly by the Super Admin who creates this
     * account (see PersonServiceImpl.createPerson) — there is no public self-registration,
     * so a null password only ever means a legacy row that predates that requirement (e.g.
     * seeded before auth existed), which simply can't authenticate until a Super Admin
     * gives it a password via createPerson or sendPasswordReset.
     */
    private String password;

    /**
     * Whether this account's email address is considered proven. There is no public
     * self-registration OTP step anymore — a Super Admin creating a person here vouches for
     * the address directly, so createPerson sets this true immediately. @ColumnDefault
     * backfills every EXISTING row to true when this column is first added (they predate
     * this feature, exempted per the "leave old accounts alone" decision). The OTP
     * verification machinery (otpCode/otpExpiresAt, VerifyEmailPage) stays in place for
     * whatever legacy/unverified rows still exist, but nothing sets this false going
     * forward.
     */
    @ColumnDefault("true")
    @Column(nullable = false)
    private boolean emailVerified = false;

    /** One-time verification code, null once verified (or not applicable). */
    private String otpCode;

    /** When otpCode expires — also doubles as the basis for the resend cooldown in
     *  AuthServiceImpl (no separate "last sent" column needed). */
    private LocalDateTime otpExpiresAt;

    /** One-time "forgot password" code, null once used (or never requested). Kept separate
     *  from otpCode/otpExpiresAt above — those verify a brand-new signup's email address, this
     *  proves ownership of an already-claimed account well after that, and an in-flight
     *  signup verification should never collide with an in-flight password reset. */
    private String resetCode;

    /** Same cooldown-without-an-extra-column trick as otpExpiresAt, applied to resetCode. */
    private LocalDateTime resetCodeExpiresAt;

    /**
     * Whether this account can log in at all. A Super Admin can deactivate an account
     * without deleting it — their historical tasks/comments/reassignments stay intact,
     * login just stops working. Defaults true for everyone, old and new — nobody starts
     * out deactivated.
     */
    @ColumnDefault("true")
    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
