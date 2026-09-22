package com.throughline.taskmanagement.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/** A reusable "Source Detail" suggestion for a given source category — e.g. "BNR" under
 *  Regulator, "E&Y" under Auditor. Source Detail itself stays free text everywhere (this
 *  never restricts what can be typed); an entry here is only ever a saved, pickable
 *  suggestion so the next person creating a task doesn't have to retype it. Who may add one
 *  is enforced in TaskSourceEntryServiceImpl (Executive-or-above), not here. */
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
