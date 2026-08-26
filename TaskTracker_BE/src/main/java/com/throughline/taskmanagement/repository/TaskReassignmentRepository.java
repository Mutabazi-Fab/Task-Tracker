package com.throughline.taskmanagement.repository;

import com.throughline.taskmanagement.model.TaskReassignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskReassignmentRepository extends JpaRepository<TaskReassignment, Long> {
    List<TaskReassignment> findByTaskIdOrderByReassignedAtAsc(Long taskId);
}
