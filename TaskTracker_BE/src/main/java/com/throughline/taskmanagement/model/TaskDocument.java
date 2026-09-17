package com.throughline.taskmanagement.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * A supporting document attached to a task — a memo, a directive, a spec, anything the
 * creator (or anyone else, later — see TaskServiceImpl.addDocument) wants on record
 * alongside the task itself. Visible to whoever can already see the task (task-detail
 * viewing has no per-viewer restriction in this app — see TaskServiceImpl.getTaskById), so
 * this needs no separate permission model of its own.
 *
 * content is stored as plain bytes in Postgres (bytea), not on local disk and not via
 * @Lob — a bare byte[] field already maps to bytea directly in modern Hibernate; @Lob risks
 * mapping to Postgres's separate large-object (oid) type instead. Keeping the file inside
 * the same transaction/backup/cascade-delete story as everything else in this app, rather
 * than introducing a filesystem upload directory this app has never needed before.
 */
@Entity
@Table(name = "task_documents", indexes = {
        @Index(name = "idx_task_documents_task_id", columnList = "task_id")
})
@Getter
@Setter
public class TaskDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    @JsonIgnore
    private Task task;

    @NotBlank
    @Column(nullable = false)
    private String fileName;

    @NotBlank
    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private long fileSize;

    @Column(nullable = false)
    private byte[] content;

    @ManyToOne(optional = false)
    @JoinColumn(name = "uploaded_by_id", nullable = false)
    private Person uploadedBy;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime uploadedAt;
}
