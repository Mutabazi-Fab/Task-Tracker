package com.throughline.taskmanagement.repository;

import com.throughline.taskmanagement.enums.Role;
import com.throughline.taskmanagement.model.Person;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PersonRepository extends JpaRepository<Person, Long> {
    Optional<Person> findByEmail(String email);

    boolean existsByEmail(String email);

    // Used for the "last Super Admin" guard — you can't demote/deactivate the only one
    // left, since nobody would then be able to ever grant that role again.
    long countByRoleAndActiveTrue(Role role);

    // No findByTeamId — a person can belong to multiple teams now, so "which team is this
    // person in" is no longer a single-valued question. Look up via TeamMemberRepository instead.

    @Query("SELECT p FROM Person p WHERE LOWER(p.fullName) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(p.jobTitle) LIKE LOWER(CONCAT('%', :q, '%'))")
    List<Person> search(@Param("q") String q);

    Optional<Person> findByEmailIgnoreCase(String email);

    // Everyone who shares at least one team with personId (that person included) — the
    // "teammates" a Member is allowed to see on the People page. Someone in zero teams
    // matches nothing here, which is the intended "individual sees no one" behavior.
    @Query("SELECT DISTINCT tm.person FROM TeamMember tm WHERE tm.team.id IN " +
            "(SELECT tm2.team.id FROM TeamMember tm2 WHERE tm2.person.id = :personId)")
    Page<Person> findTeammatesOf(@Param("personId") Long personId, Pageable pageable);
}
