package com.throughline.taskmanagement.model;

import com.throughline.taskmanagement.enums.AssigneeType;
import com.throughline.taskmanagement.enums.CreatedByRole;
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

    @ManyToOne(optional = false)
    @JoinColumn(name = "assigned_by_id", nullable = false)
    private Person assignedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssigneeType assigneeType;

    @ManyToOne
    @JoinColumn(name = "assigned_person_id")
    private Person assignedPerson;

    @ManyToOne
    @JoinColumn(name = "assigned_team_id")
    private Team assignedTeam;

    /**
     * Null = top-level (Director-only, always team-assigned). Non-null = a subtask
     * (created by the Team Leader of the owning team, or the Director directly;
     * always individual-assigned). Strictly two levels — the service layer rejects
     * giving a subtask its own children.
     */
    @ManyToOne
    @JoinColumn(name = "parent_task_id")
    private Task parentTask;

    @OneToMany(mappedBy = "parentTask", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<Task> subtasks = new ArrayList<>();

    /** Who structured this piece of work — see {@link CreatedByRole}. Nullable only for
     *  tasks created before the hierarchy existed. */
    @Enumerated(EnumType.STRING)
    private CreatedByRole createdByRole;

    @Column(nullable = false)
    private LocalDate dateAssigned;

    /**
     * For a subtask: set only via addProgressComment (the comment's percentage), exactly
     * as before hierarchy existed. For a top-level task: NEVER set by a comment — it's the
     * average of this task's subtasks' percentages (0 if it has none yet), recalculated by
     * TaskServiceImpl.recalculateParentRollup whenever a subtask's percentage or existence
     * changes. A comment can still be added directly to a top-level task, but it's a
     * narrative record only and never moves this value.
     */
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

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /**
     * When TaskStalenessJob last notified someone this task hadn't moved in a while — null
     * if it's never been flagged, or if it moved since the last flag (see
     * TaskServiceImpl.addProgressComment/recalculateParentRollup, which both clear this the
     * moment progress genuinely changes). Prevents re-notifying every single day for the
     * same stale stretch; a task that goes stale, gets nudged, moves, then stalls again
     * later gets a fresh notification for that new stretch.
     */
    private LocalDateTime staleAlertSentAt;
}
