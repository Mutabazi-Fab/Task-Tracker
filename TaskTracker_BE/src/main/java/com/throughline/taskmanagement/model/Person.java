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

    /** Every person belongs to exactly one Department, independent of team membership. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    /** Stored in plaintext (see SecurityConfig's NoOpPasswordEncoder, a deliberate choice for this
     *  project). */
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

    /** The shared TOTP secret (Base32), encrypted at rest — see TotpSecretConverter. */
    @Convert(converter = TotpSecretConverter.class)
    private String totpSecret;

    /** Set only once a submitted code against totpSecret is actually verified — generating the secret/QR
     *  alone does NOT set this. */
    private LocalDateTime totpEnabledAt;

    /** Short-lived, single-purpose token issued after a correct password (or password reset) when TOTP is
     *  still needed, one way or another — either to complete enrollment or to challenge an already-enrolled
     *  account. */
    private String pendingAuthToken;

    /** Same cooldown/expiry-column pattern as otpExpiresAt/resetCodeExpiresAt above. */
    private LocalDateTime pendingAuthTokenExpiresAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
