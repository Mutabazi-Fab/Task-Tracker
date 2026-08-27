package com.throughline.taskmanagement.repository;

import com.throughline.taskmanagement.model.TaskReassignment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskReassignmentRepository extends JpaRepository<TaskReassignment, Long> {
    /** Used only for the full embedded list inside TaskDetailResponse. The paginated
     *  overload backs the dedicated GET /tasks/{id}/reassignments endpoint. */
    List<TaskReassignment> findByTaskIdOrderByReassignedAtAsc(Long taskId);

    Page<TaskReassignment> findByTaskIdOrderByReassignedAtAsc(Long taskId, Pageable pageable);
}
