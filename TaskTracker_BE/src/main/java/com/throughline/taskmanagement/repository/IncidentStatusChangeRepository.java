package com.throughline.taskmanagement.repository;

import com.throughline.taskmanagement.model.IncidentStatusChange;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncidentStatusChangeRepository extends JpaRepository<IncidentStatusChange, Long> {
    List<IncidentStatusChange> findByIncidentIdOrderByChangedAtAsc(Long incidentId);
}
