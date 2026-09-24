package com.throughline.taskmanagement.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/** One of up to 3 tasks a Member has flagged as "what I'm focused on today" — purely a personal,
 *  self-set pointer (see PersonServiceImpl.addDailyGoal/removeDailyGoal), never an assignment
 *  mechanism. */
@Entity
@Table(name = "person_daily_goals")
@Getter
@Setter
public class PersonDailyGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    /** Must already be one of this person's own assigned tasks — checked in
     *  PersonServiceImpl.addDailyGoal, not just trusted here. */
    @ManyToOne(optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime addedAt;
}
