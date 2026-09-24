package com.throughline.taskmanagement.model;

import com.throughline.taskmanagement.enums.AssigneeType;
import com.throughline.taskmanagement.enums.CreatedByRole;
import com.throughline.taskmanagement.enums.TaskSeverity;
import com.throughline.taskmanagement.enums.TaskStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tasks", indexes = {
        @Index(name = "idx_tasks_assigned_person_id", columnList = "assigned_person_id"),
        @Index(name = "idx_tasks_assigned_team_id", columnList = "assigned_team_id"),
        @Index(name = "idx_tasks_assigned_by_id", columnList = "assigned_by_id"),
        @Index(name = "idx_tasks_parent_task_id", columnList = "parent_task_id"),
        @Index(name = "idx_tasks_status", columnList = "status")
})
@Getter
@Setter
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String taskCode;

    @NotBlank
    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    // Every ManyToOne on this entity is explicitly LAZY: Task self-references via parentTask, and
    // assignedTeam/assignedDepartment/assignedPerson/assignedBy all chain back into Person <-> Department,
    // which itself cycles (Person.department -> Department.headDirector/createdBy -> Person -> Department
    // -> ...).
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by_id", nullable = false)
    private Person assignedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssigneeType assigneeType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_person_id")
    private Person assignedPerson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_team_id")
    private Team assignedTeam;

    /** Set only when assigneeType is DEPARTMENT. Null for every TEAM/INDIVIDUAL task, same
     *  XOR-enforced-in-code approach as assignedTeam/assignedPerson. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_department_id")
    private Department assignedDepartment;

    /** Null = top-level task. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_task_id")
    private Task parentTask;

    /** 0 for top-level, 1 for a direct child, 2 for a grandchild (DEPARTMENT-rooted only).
     *  Set once at creation and never changed — a task is reassigned, never re-parented. */
    @Column(nullable = false)
    private int depth = 0;

    @OneToMany(mappedBy = "parentTask", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<Task> subtasks = new ArrayList<>();

    /** Who structured this piece of work — see {@link CreatedByRole}. Nullable only for
     *  legacy tasks created before the hierarchy existed. */
    @Enumerated(EnumType.STRING)
    private CreatedByRole createdByRole;

    @Column(nullable = false)
    private LocalDate dateAssigned;

    /** Required at creation going forward, but nullable at the DB level for legacy tasks. */
    @Column
    private LocalDate deadline;

    /** Who actually originated this task, settable by whoever creates it at any depth. */
    @Column(length = 100)
    private String source;

    /** Free text alongside source (e.g. "Director Maj. Musoni", "GPO", "E&Y") — not a
     *  Person FK, since these are often external/organizational. Null unless source is set. */
    @Column(length = 200)
    private String sourceLabel;

    /** Executive-only, settable at creation only — see TaskServiceImpl.createTask/
     *  createLeafSubtask/createImplementationTask. */
    @Enumerated(EnumType.STRING)
    private TaskSeverity severity;

    /** Manual toggle (see TaskService.setPinned, Director-or-above), NOT hard-derived from severity. */
    @Column(nullable = false)
    private boolean pinned = false;

    /** For a subtask: set via addProgressComment. */
    @Column(nullable = false)
    private int progressPercentage = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status = TaskStatus.PENDING;

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<TaskComment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("reassignedAt ASC")
    private List<TaskReassignment> reassignments = new ArrayList<>();

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("requestedAt ASC")
    private List<TaskDeadlineExtensionRequest> deadlineExtensionRequests = new ArrayList<>();

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("uploadedAt ASC")
    private List<TaskDocument> documents = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /** When TaskStalenessJob last flagged this task — null if never flagged, or cleared by
     *  addProgressComment/recalculateParentRollup once progress genuinely moves. */
    private LocalDateTime staleAlertSentAt;
}
