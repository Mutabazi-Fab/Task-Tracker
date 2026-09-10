package com.throughline.taskmanagement.repository;

import com.throughline.taskmanagement.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
    Optional<Department> findByName(String name);
    boolean existsByName(String name);
    boolean existsByHeadDirectorId(Long headDirectorId);
}
