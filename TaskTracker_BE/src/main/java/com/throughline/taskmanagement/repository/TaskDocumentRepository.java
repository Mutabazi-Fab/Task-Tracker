package com.throughline.taskmanagement.repository;

import com.throughline.taskmanagement.model.TaskDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskDocumentRepository extends JpaRepository<TaskDocument, Long> {
    List<TaskDocument> findByTaskIdOrderByUploadedAtAsc(Long taskId);
}
