package com.throughline.taskmanagement.model;

import com.throughline.taskmanagement.enums.AssigneeType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "task_reassignments", indexes = {
        @Index(name = "idx_task_reassignments_task_id", columnList = "task_id"),
        @Index(name = "idx_task_reassignments_from_person_id", columnList = "from_person_id")
})
@Getter
@Setter
public class TaskReassignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @Enumerated(EnumType.STRING)
    private AssigneeType fromAssigneeType;

    @ManyToOne
    @JoinColumn(name = "from_person_id")
    private Person fromPerson;

    @ManyToOne
    @JoinColumn(name = "from_team_id")
    private Team fromTeam;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssigneeType toAssigneeType;

    @ManyToOne
    @JoinColumn(name = "to_person_id")
    private Person toPerson;

    @ManyToOne
    @JoinColumn(name = "to_team_id")
    private Team toTeam;

    @ManyToOne(optional = false)
    @JoinColumn(name = "reassigned_by_id", nullable = false)
    private Person reassignedBy;

    @NotBlank
    @Column(length = 1000, nullable = false)
    private String reason;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime reassignedAt;
}
