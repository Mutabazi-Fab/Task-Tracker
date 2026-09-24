package com.throughline.taskmanagement.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.throughline.taskmanagement.enums.CommentType;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "task_comments", indexes = {
        @Index(name = "idx_task_comments_task_id", columnList = "task_id"),
        @Index(name = "idx_task_comments_author_id", columnList = "author_id")
})
@Getter
@Setter
public class TaskComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    @JsonIgnore
    private Task task;

    @ManyToOne(optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private Person author;

    @Min(0)
    @Max(100)
    @Column(nullable = false)
    private int percentageAtComment;

    @NotBlank
    @Column(length = 2000, nullable = false)
    private String body;

    @Column(nullable = false)
    private int sequenceNumber;

    /** PROGRESS (the default, for every comment that predates this field) is a real
     *  percentage reading and counts toward the trend/timeline chart; DISCUSSION is a
     *  plain Q&A message in the open thread below it, never trusted for progress and never
     *  plotted on the trend chart — see TaskMapper/TaskServiceImpl.getTaskProgressTimeline. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CommentType type = CommentType.PROGRESS;

    /** Set only for a DISCUSSION reply — the top-level comment it replies to. */
    @ManyToOne
    @JoinColumn(name = "parent_comment_id")
    @JsonIgnore
    private TaskComment parentComment;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
