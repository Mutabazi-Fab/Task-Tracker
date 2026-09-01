package com.throughline.taskmanagement.repository;

import com.throughline.taskmanagement.model.RoleChange;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleChangeRepository extends JpaRepository<RoleChange, Long> {
    Page<RoleChange> findAllByOrderByTimestampDesc(Pageable pageable);
}
