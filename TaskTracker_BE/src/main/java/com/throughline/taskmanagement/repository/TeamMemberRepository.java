package com.throughline.taskmanagement.repository;

import com.throughline.taskmanagement.model.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {
    List<TeamMember> findByTeamId(Long teamId);

    List<TeamMember> findByPersonId(Long personId);

    Optional<TeamMember> findByTeamIdAndPersonId(Long teamId, Long personId);

    boolean existsByTeamIdAndPersonId(Long teamId, Long personId);

    Optional<TeamMember> findByTeamIdAndIsLeaderTrue(Long teamId);

    long countByTeamId(Long teamId);
}
