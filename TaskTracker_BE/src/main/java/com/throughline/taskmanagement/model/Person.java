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

    /** Military rank (e.g. "Major", "Captain") — optional, set only for officers, null for
     *  civilian staff. Never a required field. */
    private String rank;

    /** Authorization role. Nullable — rows created before auth existed have no role and
     *  are treated as MEMBER wherever this is checked. */
    @Enumerated(EnumType.STRING)
    private Role role;

    /** Every person belongs to exactly one Department, independent of team membership.
     *  Required in practice (PersonServiceImpl.createPerson), not a DB constraint.
     *  Nullable only for accounts that predate this field. */
    @ManyToOne
    @JoinColumn(name = "department_id")
    private Department department;

    /** Stored in plaintext (see SecurityConfig's NoOpPasswordEncoder, a deliberate choice
     *  for this project). Set by the Super Admin who creates the account — no public
     *  self-registration, so null only means a legacy row awaiting createPerson/
     *  sendPasswordReset. */
    private String password;

    /** Whether this account's email is considered proven. A Super Admin creating a person
     *  vouches for the address directly, so createPerson sets this true immediately.
     *  @ColumnDefault backfills existing rows to true (they predate this feature). The OTP
     *  machinery (otpCode/otpExpiresAt) stays for legacy unverified rows only. */
    @ColumnDefault("true")
    @Column(nullable = false)
    private boolean emailVerified = false;

    /** One-time verification code, null once verified (or not applicable). */
    private String otpCode;

    /** When otpCode expires — also doubles as the basis for the resend cooldown in
     *  AuthServiceImpl (no separate "last sent" column needed). */
    private LocalDateTime otpExpiresAt;

    /** One-time "forgot password" code, null once used (or never requested). Kept separate
     *  from otpCode/otpExpiresAt — those verify a new signup's email, this proves ownership
     *  of an already-claimed account, and the two flows must never collide. */
    private String resetCode;

    /** Same cooldown-without-an-extra-column trick as otpExpiresAt, applied to resetCode. */
    private LocalDateTime resetCodeExpiresAt;

    /** Whether this account can log in at all. A Super Admin can deactivate an account
     *  without deleting it — history stays intact, login just stops working. */
    @ColumnDefault("true")
    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
