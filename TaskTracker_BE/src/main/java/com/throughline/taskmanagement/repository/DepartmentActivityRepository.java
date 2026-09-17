package com.throughline.taskmanagement.repository;

import com.throughline.taskmanagement.model.DepartmentActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentActivityRepository extends JpaRepository<DepartmentActivity, Long> {
    // findAll(Pageable) is inherited — same convention as TaskActivityRepository.
    Page<DepartmentActivity> findAll(Pageable pageable);
}
