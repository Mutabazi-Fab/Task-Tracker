package com.throughline.taskmanagement.model;

import com.throughline.taskmanagement.enums.AccessResourceType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/** Lets one person see and act on one task or incident outside their normal scope. Granted and
 *  revoked only by an Executive or Super Admin; kept (revokedAt set) rather than deleted so there
 *  is always a record of who had access, who allowed it, and why. No expiry: a grant stays until revoked. */
@Entity
@Table(name = "access_grants", indexes = {
        @Index(name = "idx_access_grants_grantee", columnList = "grantee_id"),
        @Index(name = "idx_access_grants_resource", columnList = "resource_type, resource_id")
})
@Getter
@Setter
public class AccessGrant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "grantee_id", nullable = false)
    private Person grantee;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false)
    private AccessResourceType resourceType;

    @Column(name = "resource_id", nullable = false)
    private Long resourceId;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "granted_by_id", nullable = false)
    private Person grantedBy;

    @Column(length = 1000, nullable = false)
    private String reason;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime grantedAt;

    private LocalDateTime revokedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revoked_by_id")
    private Person revokedBy;
}
