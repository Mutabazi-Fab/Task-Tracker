package com.throughline.taskmanagement.model;

import com.throughline.taskmanagement.enums.Role;
import com.throughline.taskmanagement.security.TotpSecretConverter;
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
     *  Nullable only for accounts that predate this field. LAZY — Department.headDirector/
     *  createdBy point back to Person, so an EAGER default here (JPA's default for
     *  @ManyToOne) turns any query touching a Person into an unbounded join across that
     *  cycle. See Task.java's comment on assignedBy for the full story. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    /** Stored in plaintext (see SecurityConfig's NoOpPasswordEncoder, a deliberate choice
     *  for this project). Set by the Super Admin who creates the account, or later via
     *  PersonServiceImpl.setPasswordDirectly (the offline password-recovery path — see
     *  PasswordResetRequest) — never emailed, never OTP-verified. Null only means a
     *  pre-this-system legacy row that never went through createPerson. */
    private String password;

    /** Whether this account can log in at all. A Super Admin can deactivate an account
     *  without deleting it — history stays intact, login just stops working. */
    @ColumnDefault("true")
    @Column(nullable = false)
    private boolean active = true;

    /** Whether this account must go through TOTP 2FA to log in at all. @ColumnDefault
     *  backfills every pre-existing row to false — 2FA rollout only applies going forward,
     *  to accounts PersonServiceImpl.createPerson creates from now on; nobody who already
     *  has an account is forced into enrollment by this change. */
    @ColumnDefault("false")
    @Column(nullable = false)
    private boolean totpRequired = false;

    /** The shared TOTP secret (Base32), encrypted at rest — see TotpSecretConverter. Unlike
     *  the password, nobody ever needs to read this back as a human, so there's no
     *  "convenient to look up" tradeoff in favor of storing it in plaintext. Null means
     *  "never generated yet" (see totpEnabledAt for the actual enrolled/pending distinction). */
    @Convert(converter = TotpSecretConverter.class)
    private String totpSecret;

    /** Set only once a submitted code against totpSecret is actually verified — generating
     *  the secret/QR alone does NOT set this. Keeps an abandoned enrollment (QR shown, never
     *  confirmed) from being treated as done, and lets a repeated login attempt during
     *  enrollment reuse the same still-unconfirmed secret rather than generating a new one
     *  that would no longer match a QR the person already scanned. */
    private LocalDateTime totpEnabledAt;

    /** Short-lived, single-purpose token issued after a correct password (or password
     *  reset) when TOTP is still needed, one way or another — either to complete
     *  enrollment or to challenge an already-enrolled account. Deliberately NOT a JWT: a
     *  real session JWT is only ever issued once TOTP actually passes, so this can't be
     *  used against any other endpoint even if intercepted. */
    private String pendingAuthToken;

    /** Same cooldown/expiry-column pattern as otpExpiresAt/resetCodeExpiresAt above. */
    private LocalDateTime pendingAuthTokenExpiresAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
