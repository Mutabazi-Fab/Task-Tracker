package com.throughline.taskmanagement.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** The org-chart tier above Team — Executive/CEO-level tasks are assigned to a Department, never to a
 *  team or person directly (see Task.assignedDepartment, AssigneeType.DEPARTMENT). */
@Entity
@Table(name = "departments")
@Getter
@Setter
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(unique = true, nullable = false)
    private String name;

    /** The one Director accountable for everything assigned to this department. */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "head_director_id", nullable = false)
    private Person headDirector;

    /** Always a Super Admin — enforced in DepartmentServiceImpl, not just trusted here. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private Person createdBy;

    @OneToMany(mappedBy = "department")
    private List<Team> teams = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
