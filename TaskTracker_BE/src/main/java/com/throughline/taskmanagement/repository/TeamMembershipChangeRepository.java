package com.throughline.taskmanagement.repository;

import com.throughline.taskmanagement.model.TeamMembershipChange;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamMembershipChangeRepository extends JpaRepository<TeamMembershipChange, Long> {
    Page<TeamMembershipChange> findByTeamIdOrderByTimestampDesc(Long teamId, Pageable pageable);

    /** For the Director's "recent activity across all teams" view. */
    Page<TeamMembershipChange> findAllByOrderByTimestampDesc(Pageable pageable);
}
