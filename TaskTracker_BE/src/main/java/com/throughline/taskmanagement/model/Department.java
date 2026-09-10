package com.throughline.taskmanagement.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * The org-chart tier above Team — Executive/CEO-level tasks are assigned to a Department,
 * never to a team or person directly (see Task.assignedDepartment, AssigneeType.DEPARTMENT).
 * Every Team and every Person belongs to exactly one Department (enforced in
 * TeamServiceImpl.createTeam / PersonServiceImpl.createPerson, not a DB constraint — same
 * approach this app already takes for other required-but-not-DB-enforced invariants, e.g.
 * a Task always having exactly one of assignedTeam/assignedPerson set).
 *
 * Administration (create/rename/reassign head) is Super-Admin-only — the same tier that
 * already owns org-chart governance elsewhere (role changes, account activation). A
 * Director or Executive can read this, never restructure it.
 */
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
    @ManyToOne(optional = false)
    @JoinColumn(name = "head_director_id", nullable = false)
    private Person headDirector;

    /** Always a Super Admin — enforced in DepartmentServiceImpl, not just trusted here. */
    @ManyToOne
    @JoinColumn(name = "created_by_id")
    private Person createdBy;

    @OneToMany(mappedBy = "department")
    private List<Team> teams = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
