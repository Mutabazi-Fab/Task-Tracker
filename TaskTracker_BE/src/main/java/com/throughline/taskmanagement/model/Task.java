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

    // Every ManyToOne on this entity is explicitly LAZY: Task self-references via
    // parentTask, and assignedTeam/assignedDepartment/assignedPerson/assignedBy all chain
    // back into Person <-> Department, which itself cycles (Person.department ->
    // Department.headDirector/createdBy -> Person -> Department -> ...). Left at JPA's
    // EAGER default, a single query touching a Task (e.g. loading a TaskComment) made
    // Hibernate fold that entire cyclic graph into one giant join — harmless while the
    // column count stayed under Postgres's hard 1664-column-per-query limit, until
    // Person's TOTP columns pushed it over and the query started failing outright.
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

    /** Null = top-level task. Non-null = a subtask. Under a DEPARTMENT-rooted hierarchy a
     *  task can go two levels deep (depth 1 implementation task, depth 2 leaf subtask);
     *  any other hierarchy stays capped at depth 1. See {@link #depth}. */
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

    /** Required at creation going forward, but nullable at the DB level for legacy tasks.
     *  Extended directly by assignedBy, or via the request/approve workflow — see
     *  TaskService.requestDeadlineExtension/decideDeadlineExtension/extendDeadlineDirectly
     *  and {@link TaskDeadlineExtensionRequest}. Never touched by UpdateTaskRequest, which
     *  would bypass that audit trail. */
    @Column
    private LocalDate deadline;

    /** Who actually originated this task, settable by whoever creates it at any depth.
     *  Nullable — most tasks are ordinary internal work with no mandate to record. Open
     *  text matched against TaskSourceCategory's saved list, not a fixed enum — see
     *  TaskSourceCategoryServiceImpl for who may add a brand new category. */
    @Column(length = 100)
    private String source;

    /** Free text alongside source (e.g. "Director Maj. Musoni", "GPO", "E&Y") — not a
     *  Person FK, since these are often external/organizational. Null unless source is set. */
    @Column(length = 200)
    private String sourceLabel;

    /** Executive-only, settable at creation only — see TaskServiceImpl.createTask/
     *  createLeafSubtask/createImplementationTask. Nullable — most tasks carry no
     *  severity classification at all. */
    @Enumerated(EnumType.STRING)
    private TaskSeverity severity;

    /** Manual toggle (see TaskService.setPinned, Director-or-above), NOT hard-derived from
     *  severity. CRITICAL severity sets this true as a one-time default at creation only
     *  and is never re-enforced, so a CRITICAL task can be freely un-pinned once on track. */
    @Column(nullable = false)
    private boolean pinned = false;

    /** For a subtask: set via addProgressComment. For a top-level task: NEVER set by a
     *  comment — it's the average of its subtasks' percentages, recalculated by
     *  TaskServiceImpl.recalculateParentRollup whenever a subtask changes. */
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
     *  addProgressComment/recalculateParentRollup once progress genuinely moves. Prevents
     *  re-notifying every day for the same stale stretch. */
    private LocalDateTime staleAlertSentAt;
}
