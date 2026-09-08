package com.throughline.taskmanagement.repository;

import com.throughline.taskmanagement.model.TaskActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskActivityRepository extends JpaRepository<TaskActivity, Long> {
    // findAll(Pageable) is inherited — the controller passes ?sort=timestamp,desc, same
    // convention as the role-change/account-status-change activity endpoints.
    Page<TaskActivity> findAll(Pageable pageable);
}
