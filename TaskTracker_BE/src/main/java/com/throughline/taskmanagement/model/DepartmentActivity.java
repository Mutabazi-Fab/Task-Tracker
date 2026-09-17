package com.throughline.taskmanagement.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Append-only log of department deletions — mirrors TaskActivity's snapshot pattern: the
 * department no longer exists by the time anyone reads this, so its name is stored as a
 * plain string rather than a live FK. Visible to a Director or Super Admin, same tier as
 * task activity (see DepartmentServiceImpl.getDepartmentActivity).
 */
@Entity
@Table(name = "department_activities")
@Getter
@Setter
public class DepartmentActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String departmentName;

    /** Who deleted it — Person rows are never hard-deleted (only deactivated), so a live FK
     *  here is safe unlike one to Department. */
    @ManyToOne(optional = false)
    @JoinColumn(name = "performed_by_id", nullable = false)
    private Person performedBy;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime timestamp;
}
