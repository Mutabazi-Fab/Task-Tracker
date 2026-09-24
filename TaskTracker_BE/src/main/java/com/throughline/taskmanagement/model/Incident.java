package com.throughline.taskmanagement.model;

import com.throughline.taskmanagement.enums.DataBreachStatus;

import com.throughline.taskmanagement.enums.IncidentCategory;
import com.throughline.taskmanagement.enums.IncidentReviewStatus;
import com.throughline.taskmanagement.enums.IncidentSeverity;
import com.throughline.taskmanagement.enums.IncidentStatus;
import com.throughline.taskmanagement.enums.RegulatorNotifiableStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * The bank's former "IT. Incident Register.xlsx" workbook, rebuilt as a first-class module
 * of this app. Every column maps to a field here except the workbook's own computed columns
 * (Inherent Score, Severity, Net Loss, Days Open, Action SLA) — see the per-field comments
 * for which of those are persisted (recomputed server-side on every write, never trusted
 * from a request) versus computed fresh at read time (see IncidentMapper) because they
 * depend on TODAY().
 *
 * Every @ManyToOne is explicitly LAZY, same reasoning as Task's own fields — see Task.java's
 * class-level comment on the eager-fetch/1664-column incident this codebase already hit
 * once.
 */
@Entity
@Table(name = "incidents", indexes = {
        @Index(name = "idx_incidents_status", columnList = "status"),
        @Index(name = "idx_incidents_severity", columnList = "severity"),
        @Index(name = "idx_incidents_category", columnList = "category"),
        @Index(name = "idx_incidents_reported_by_id", columnList = "reported_by_id"),
        @Index(name = "idx_incidents_action_owner_id", columnList = "action_owner_id")
})
@Getter
@Setter
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Server-generated (e.g. INC-2026-005), never client-typed — avoids the duplicate/
     *  malformed ids a free-typed Excel column allowed. See IncidentServiceImpl.generateIncidentCode. */
    @Column(unique = true, nullable = false)
    private String incidentCode;

    @Column(nullable = false)
    private LocalDate dateOccurred;

    private LocalTime timeOccurred;

    @Column(nullable = false)
    private LocalDate dateDiscovered;

    @Column(nullable = false)
    private LocalDate dateReported;

    /** The name of a {@link Department} (or "Other"), stored as plain text rather than a FK or
     *  enum: the dropdown is driven by whatever Departments currently exist, so a
     *  department created later shows up here automatically, and an incident keeps its
     *  original wording even if that department is later renamed or removed. Rows recorded
     *  before this change hold the old enum constant instead (e.g. "CYBERSECURITY") —
     *  IncidentMapper.displayBusinessUnit turns those into readable text at read time, and
     *  IncidentRepository.search matches both spellings, so no existing row needs rewriting. */
    @Column(nullable = false, length = 150)
    private String businessUnit;

    private String locationChannel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncidentCategory category;

    private String eventType;

    @NotBlank
    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    /** 1-5, nullable — not every incident is scored the moment it's reported. */
    private Integer likelihood;

    /** 1-5, nullable — see likelihood. */
    private Integer impact;

    /** = likelihood * impact. Persisted for querying/sorting, but always recomputed
     *  server-side from likelihood/impact on every create/update — see
     *  IncidentServiceImpl.applyDerivedRiskFields. Null exactly when likelihood or impact is null. */
    private Integer inherentScore;

    /** Banded from inherentScore — see IncidentSeverity. Same recompute-only-server-side
     *  rule as inherentScore. Null exactly when inherentScore is null (shown as "Not yet
     *  scored" in the UI instead of the source Excel's literal FALSE-when-blank bug). */
    @Enumerated(EnumType.STRING)
    private IncidentSeverity severity;

    private Integer customersAffected;

    private Integer serviceDowntimeMinutes;

    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal grossLoss = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal recovery = BigDecimal.ZERO;

    /** = MAX(0, grossLoss - recovery). Recomputed server-side on every write, same rule as
     *  inherentScore/severity. */
    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal netLoss = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DataBreachStatus dataBreach = DataBreachStatus.NO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RegulatorNotifiableStatus regulatorNotifiable = RegulatorNotifiableStatus.NO;

    private LocalDate bnrNotificationDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncidentStatus status = IncidentStatus.OPEN;

    @Column(length = 2000)
    private String immediateContainment;

    @Column(length = 2000)
    private String rootCause;

    @Column(length = 2000)
    private String correctiveAction;

    /** A real system user, not free text — enables notifying them directly. See
     *  IncidentServiceImpl for the notification sent when this is set/changed. Nullable
     *  until an owner is assigned during investigation. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "action_owner_id")
    private Person actionOwner;

    private LocalDate targetClosureDate;

    private LocalDate actualClosureDate;

    private String evidenceReference;

    /** Always the caller's own JWT-resolved identity at creation — never trusted from the
     *  request body. See IncidentController.createIncident. */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_by_id", nullable = false)
    private Person reportedBy;

    /** Deliberately free text, NOT a Person FK — unlike actionOwner/reportedBy. A Director
     *  reporting an incident often needs to name whoever in their department caused or is
     *  associated with it, who may not be the person best placed to action the fix (that's
     *  actionOwner) and may not even need to be identified as precisely as a system-user
     *  lookup would require. */
    private String incidentOwner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncidentReviewStatus riskReview = IncidentReviewStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncidentReviewStatus complianceReview = IncidentReviewStatus.PENDING;

    @Column(length = 2000)
    private String lessonsLearned;

    @OneToMany(mappedBy = "incident", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("changedAt ASC")
    private List<IncidentStatusChange> statusChanges = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
