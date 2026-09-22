package com.throughline.taskmanagement.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/** One entry in the Source dropdown itself (e.g. "Initiative", "Regulator") — an open,
 *  saved list rather than a fixed enum, so an Executive/Super Admin can add a brand new
 *  category (see TaskSourceCategoryServiceImpl). addedBy is null for the 4 built-in
 *  categories seeded at launch, which predate this feature and have no real creator. */
@Entity
@Table(name = "task_source_categories")
@Getter
@Setter
public class TaskSourceCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @ManyToOne
    @JoinColumn(name = "added_by_id")
    private Person addedBy;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
