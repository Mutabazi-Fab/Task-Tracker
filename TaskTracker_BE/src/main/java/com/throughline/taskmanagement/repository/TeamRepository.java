package com.throughline.taskmanagement.repository;

import com.throughline.taskmanagement.model.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, Long> {
    Optional<Team> findByName(String name);
    boolean existsByName(String name);

    // Backs DepartmentServiceImpl.deleteDepartment's safety check — a department can't be
    // deleted while any team still points at it (Team.department is required, not
    // nullable), so this must come back empty first.
    boolean existsByDepartmentId(Long departmentId);
}
