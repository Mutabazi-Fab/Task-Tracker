package com.throughline.taskmanagement.repository;

import com.throughline.taskmanagement.enums.TaskSource;
import com.throughline.taskmanagement.model.TaskSourceEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaskSourceEntryRepository extends JpaRepository<TaskSourceEntry, Long> {

    List<TaskSourceEntry> findBySourceOrderByLabelAsc(TaskSource source);

    // Case-insensitive so "bnr" and "BNR" are treated as the same suggestion — see
    // TaskSourceEntryServiceImpl.addEntry's idempotent-add behavior.
    Optional<TaskSourceEntry> findBySourceAndLabelIgnoreCase(TaskSource source, String label);
}
