package com.throughline.taskmanagement.repository;

import com.throughline.taskmanagement.model.IncidentGuidanceNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncidentGuidanceNoteRepository extends JpaRepository<IncidentGuidanceNote, Long> {
    List<IncidentGuidanceNote> findAllByOrderByCreatedAtDesc();
}
