package com.throughline.taskmanagement.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/** A reusable "Source Detail" suggestion for a given source category — e.g. "BNR" under Regulator,
 *  "E&Y" under Auditor. */
@Entity
@Table(name = "task_source_entries")
@Getter
@Setter
public class TaskSourceEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Matches TaskSourceCategory.name, not a foreign key — a category can be renamed/
    // removed without needing to cascade into every entry that referenced it by string.
    @Column(nullable = false, length = 100)
    private String source;

    @NotBlank
    @Column(nullable = false, length = 200)
    private String label;

    @ManyToOne(optional = false)
    @JoinColumn(name = "added_by_id", nullable = false)
    private Person addedBy;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
