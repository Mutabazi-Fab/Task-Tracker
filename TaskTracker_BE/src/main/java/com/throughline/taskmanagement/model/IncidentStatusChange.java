package com.throughline.taskmanagement.model;

import com.throughline.taskmanagement.enums.IncidentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/** Append-only audit trail for Incident.status — the source Excel had no history at all, just one cell
 *  anyone could overwrite. */
@Entity
@Table(name = "incident_status_changes", indexes = {
        @Index(name = "idx_incident_status_changes_incident_id", columnList = "incident_id")
})
@Getter
@Setter
public class IncidentStatusChange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;

    /** Null only for the first row, recorded at creation (no prior status to record). */
    @Enumerated(EnumType.STRING)
    private IncidentStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncidentStatus toStatus;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by_id", nullable = false)
    private Person changedBy;

    private String note;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime changedAt;
}
