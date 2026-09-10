package com.throughline.taskmanagement.repository;

import com.throughline.taskmanagement.model.TaskDeadlineExtensionRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskDeadlineExtensionRequestRepository extends JpaRepository<TaskDeadlineExtensionRequest, Long> {
    Page<TaskDeadlineExtensionRequest> findByTaskIdOrderByRequestedAtDesc(Long taskId, Pageable pageable);
}
