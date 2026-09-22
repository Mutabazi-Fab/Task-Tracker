package com.throughline.taskmanagement.repository;

import com.throughline.taskmanagement.model.TaskSourceCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaskSourceCategoryRepository extends JpaRepository<TaskSourceCategory, Long> {

    List<TaskSourceCategory> findAllByOrderByNameAsc();

    // Case-insensitive so "regulator" and "Regulator" are treated as the same category —
    // see TaskSourceCategoryServiceImpl.addCategory's idempotent-add behavior.
    Optional<TaskSourceCategory> findByNameIgnoreCase(String name);
}
