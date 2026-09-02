package com.throughline.taskmanagement.repository;

import com.throughline.taskmanagement.model.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {
    List<TeamMember> findByTeamId(Long teamId);

    List<TeamMember> findByPersonId(Long personId);

    Optional<TeamMember> findByTeamIdAndPersonId(Long teamId, Long personId);

    boolean existsByTeamIdAndPersonId(Long teamId, Long personId);

    Optional<TeamMember> findByTeamIdAndIsLeaderTrue(Long teamId);

    long countByTeamId(Long teamId);

    // Do these two people share at least one team? Backs PersonServiceImpl's
    // "can this viewer see this profile" check — the same "teammate" relationship
    // PersonRepository.findTeammatesOf already uses for the People list.
    @Query("SELECT COUNT(tm) > 0 FROM TeamMember tm WHERE tm.person.id = :personId AND tm.team.id IN "
            + "(SELECT tm2.team.id FROM TeamMember tm2 WHERE tm2.person.id = :otherPersonId)")
    boolean existsSharedTeam(@Param("personId") Long personId, @Param("otherPersonId") Long otherPersonId);
}
