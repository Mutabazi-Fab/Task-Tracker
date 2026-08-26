package com.throughline.taskmanagement.repository;

import com.throughline.taskmanagement.model.TaskComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaskCommentRepository extends JpaRepository<TaskComment, Long> {
    List<TaskComment> findByTaskIdOrderByCreatedAtAsc(Long taskId);
    
    Optional<TaskComment> findFirstByTaskIdOrderByCreatedAtDesc(Long taskId);
    
    long countByTaskId(Long taskId);
    
    long countByAuthorId(Long authorId);
}
