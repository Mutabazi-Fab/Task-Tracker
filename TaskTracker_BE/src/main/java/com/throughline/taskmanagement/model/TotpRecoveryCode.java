package com.throughline.taskmanagement.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * One single-use recovery code, generated in a batch the moment TOTP enrollment is
 * confirmed (never before — an abandoned enrollment never got a batch). codeHash goes
 * through the same PasswordEncoder bean as Person.password (currently NoOp, same
 * documented dev-only tradeoff in SecurityConfig — swapping that encoder to BCrypt later
 * hashes these too, with no code change here). usedAt turns a code permanently dead the
 * moment it's spent, rather than deleting the row, so there's a record of when a phone was
 * actually lost/replaced.
 */
@Entity
@Table(name = "totp_recovery_codes", indexes = {
        @Index(name = "idx_totp_recovery_codes_person_id", columnList = "person_id")
})
@Getter
@Setter
public class TotpRecoveryCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    @Column(nullable = false)
    private String codeHash;

    private LocalDateTime usedAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
