package com.throughline.taskmanagement.repository;

import com.throughline.taskmanagement.model.TaskComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaskCommentRepository extends JpaRepository<TaskComment, Long> {
    /** Used only where the full history is genuinely needed in one shot (e.g. the embedded
     *  list inside TaskDetailResponse). The paginated overload below backs the dedicated
     *  GET /tasks/{id}/comments endpoint. */
    List<TaskComment> findByTaskIdOrderByCreatedAtAsc(Long taskId);

    Page<TaskComment> findByTaskIdOrderByCreatedAtAsc(Long taskId, Pageable pageable);

    Optional<TaskComment> findFirstByTaskIdOrderByCreatedAtDesc(Long taskId);

    long countByTaskId(Long taskId);

    long countByAuthorId(Long authorId);
}
