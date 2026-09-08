package com.throughline.taskmanagement.model;

import com.throughline.taskmanagement.enums.AssigneeType;
import com.throughline.taskmanagement.enums.TaskActivityAction;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Append-only audit log of every top-level task/subtask creation and deletion — mirrors
 * RoleChange/AccountStatusChange's pattern (never updated, only ever inserted). Visible to
 * a Director or Super Admin (see TaskServiceImpl.getTaskActivity), unlike those two logs
 * which are Super-Admin-only — a Director creating and later deleting their own mistaken
 * task is exactly the kind of thing this exists to make visible, to them and above.
 *
 * Everything about the task is snapshotted onto this row rather than held as a live FK to
 * Task: a DELETED entry's task no longer exists by the time anyone reads this, and even a
 * CREATED entry should keep showing the task's details as they were at creation time, not
 * whatever the task has since been renamed/reassigned to.
 */
@Entity
@Table(name = "task_activities", indexes = {
        @Index(name = "idx_task_activities_task_code", columnList = "task_code")
})
@Getter
@Setter
public class TaskActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskActivityAction action;

    @Column(nullable = false)
    private String taskCode;

    @Column(nullable = false)
    private String title;

    /** Null for a top-level task; set for a subtask, so the log can say "under TSK-0013"
     *  even after TSK-0013 itself might later be gone too. */
    private String parentTaskCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssigneeType assigneeType;

    /** The assigned team's name or assigned person's full name at the time of this event —
     *  a snapshot, same reasoning as taskCode/title above. */
    @Column(nullable = false)
    private String assigneeSummary;

    /** Who created or deleted it — Person rows are never hard-deleted (only deactivated),
     *  so a live FK here is safe unlike one to Task. */
    @ManyToOne(optional = false)
    @JoinColumn(name = "performed_by_id", nullable = false)
    private Person performedBy;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime timestamp;
}
