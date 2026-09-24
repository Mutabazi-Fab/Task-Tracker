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

    // Backs the org-wide broadcasts (a new team/department created, a task deleted) that
    // notify every Director-or-above person except whoever did it — pass
    // List.of(Role.DIRECTOR, Role.EXECUTIVE, Role.SUPER_ADMIN).
    List<Person> findByRoleIn(List<Role> roles);

    // Backs DepartmentServiceImpl.deleteDepartment's safety check — a department can't be
    // deleted while any person is still directly assigned to it.
    boolean existsByDepartmentId(Long departmentId);

    // No findByTeamId — a person can belong to multiple teams now, so "which team is this
    // person in" is no longer a single-valued question. Look up via TeamMemberRepository instead.

    @Query("SELECT p FROM Person p WHERE LOWER(p.fullName) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(p.jobTitle) LIKE LOWER(CONCAT('%', :q, '%'))")
    List<Person> search(@Param("q") String q);

    Optional<Person> findByEmailIgnoreCase(String email);

    // Looks up whoever a still-mid-login pendingAuthToken belongs to, for the TOTP
    // setup/verify endpoints — those take the token instead of an email, since at that
    // point the caller has already proven the password but doesn't have a real session yet.
    Optional<Person> findByPendingAuthToken(String pendingAuthToken);

    // Everyone who shares at least one team with personId (that person included) — the "teammates" a
    // Member is allowed to see on the People page.
    @Query("SELECT DISTINCT tm.person FROM TeamMember tm WHERE tm.team.id IN " +
            "(SELECT tm2.team.id FROM TeamMember tm2 WHERE tm2.person.id = :personId)")
    Page<Person> findTeammatesOf(@Param("personId") Long personId, Pageable pageable);
}
