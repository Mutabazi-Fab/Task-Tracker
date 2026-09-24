package com.throughline.taskmanagement.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/** A free-text guidance entry a Director/Executive/Super Admin can add on top of the fixed reference
 *  material carried over from the source Excel's "Lists & Guidance" sheet (the severity table, the
 *  likelihood/impact scale, and the minimum-completion checklist — see
 *  IncidentGuidanceServiceImpl.getReference, which is hardcoded and NOT editable, since it's the same
 *  text the app's own closure-enforcement logic is built from). */
@Entity
@Table(name = "incident_guidance_notes")
@Getter
@Setter
public class IncidentGuidanceNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 200)
    private String title;

    @NotBlank
    @Column(nullable = false, length = 4000)
    private String body;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    private Person createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_id")
    private Person updatedBy;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
