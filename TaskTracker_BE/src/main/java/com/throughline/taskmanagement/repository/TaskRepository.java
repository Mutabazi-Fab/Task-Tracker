package com.throughline.taskmanagement.repository;

import com.throughline.taskmanagement.enums.TaskStatus;
import com.throughline.taskmanagement.model.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    Optional<Task> findByTaskCode(String taskCode);
    
    Page<Task> findByStatus(TaskStatus status, Pageable pageable);

    List<Task> findByAssignedPersonId(Long personId);

    // Backs "my tasks" — a Member's scoped view of GET /tasks: everything they're currently
    // responsible for, either assigned to them directly (individual tasks/subtasks) or as a
    // top-level task assigned to a team they belong to (not the org's whole task list, and
    // not just their individual work in isolation from their team's).
    @Query("SELECT t FROM Task t WHERE t.assignedPerson.id = :personId "
            + "OR t.assignedTeam.id IN (SELECT tm.team.id FROM TeamMember tm WHERE tm.person.id = :personId)")
    Page<Task> findVisibleToPerson(@Param("personId") Long personId, Pageable pageable);

    @Query("SELECT t FROM Task t WHERE (t.assignedPerson.id = :personId "
            + "OR t.assignedTeam.id IN (SELECT tm.team.id FROM TeamMember tm WHERE tm.person.id = :personId)) "
            + "AND t.status = :status")
    Page<Task> findVisibleToPersonAndStatus(@Param("personId") Long personId, @Param("status") TaskStatus status, Pageable pageable);

    List<Task> findByAssignedTeamId(Long teamId);
    
    List<Task> findByAssignedById(Long personId);

    List<Task> findByParentTaskId(Long parentTaskId);

    Page<Task> findByParentTaskIsNull(Pageable pageable);

    // Backs the Director's Dashboard default view: only the top-level tasks THIS Director
    // created, not the whole org's tasks. assignedBy doubles as "creator" for top-level
    // tasks (see TaskServiceImpl.createTask).
    Page<Task> findByParentTaskIsNullAndAssignedById(Long assignedById, Pageable pageable);

    long countByStatus(TaskStatus status);
    
    // List-returning: used internally by DashboardService.globalSearch, which stays
    // unpaginated per the "leave the low-risk/bounded dashboard endpoints as-is" decision.
    @Query("SELECT t FROM Task t WHERE LOWER(t.taskCode) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(t.title) LIKE LOWER(CONCAT('%', :q, '%'))")
    List<Task> search(@Param("q") String q);

    // Page-returning: backs the dedicated GET /tasks/search endpoint, which grows with the
    // org's total task count.
    @Query(value = "SELECT t FROM Task t WHERE LOWER(t.taskCode) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(t.title) LIKE LOWER(CONCAT('%', :q, '%'))",
           countQuery = "SELECT COUNT(t) FROM Task t WHERE LOWER(t.taskCode) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(t.title) LIKE LOWER(CONCAT('%', :q, '%'))")
    Page<Task> search(@Param("q") String q, Pageable pageable);

    // Same search, scoped to what's visible to this person (see findVisibleToPerson) — a
    // Member's search shouldn't surface tasks that aren't theirs or their team's any more
    // than the plain list should.
    @Query(value = "SELECT t FROM Task t WHERE (t.assignedPerson.id = :personId "
                  + "OR t.assignedTeam.id IN (SELECT tm.team.id FROM TeamMember tm WHERE tm.person.id = :personId)) AND "
                  + "(LOWER(t.taskCode) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(t.title) LIKE LOWER(CONCAT('%', :q, '%')))",
           countQuery = "SELECT COUNT(t) FROM Task t WHERE (t.assignedPerson.id = :personId "
                  + "OR t.assignedTeam.id IN (SELECT tm.team.id FROM TeamMember tm WHERE tm.person.id = :personId)) AND "
                  + "(LOWER(t.taskCode) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(t.title) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<Task> searchVisibleToPerson(@Param("q") String q, @Param("personId") Long personId, Pageable pageable);

    // List-returning: used internally by PersonService.getPersonStatistics for an aggregate
    // (tasksHandedOff) over ALL connected tasks — pagination would silently under-count there.
    @Query("SELECT DISTINCT t FROM Task t " +
           "LEFT JOIN t.reassignments r " +
           "LEFT JOIN t.comments c " +
           "WHERE t.assignedPerson.id = :personId " +
           "OR t.assignedTeam.id IN (SELECT tm.team.id FROM TeamMember tm WHERE tm.person.id = :personId) " +
           "OR r.fromPerson.id = :personId " +
           "OR c.author.id = :personId")
    List<Task> findTasksConnectedToPerson(@Param("personId") Long personId);

    // Page-returning: backs the dedicated GET /people/{id}/tasks endpoint. Explicit countQuery
    // because the automatic one Spring Data would derive can't safely replicate COUNT(DISTINCT ...)
    // over this query's joins.
    @Query(value = "SELECT DISTINCT t FROM Task t " +
           "LEFT JOIN t.reassignments r " +
           "LEFT JOIN t.comments c " +
           "WHERE t.assignedPerson.id = :personId " +
           "OR t.assignedTeam.id IN (SELECT tm.team.id FROM TeamMember tm WHERE tm.person.id = :personId) " +
           "OR r.fromPerson.id = :personId " +
           "OR c.author.id = :personId",
           countQuery = "SELECT COUNT(DISTINCT t) FROM Task t " +
           "LEFT JOIN t.reassignments r " +
           "LEFT JOIN t.comments c " +
           "WHERE t.assignedPerson.id = :personId " +
           "OR t.assignedTeam.id IN (SELECT tm.team.id FROM TeamMember tm WHERE tm.person.id = :personId) " +
           "OR r.fromPerson.id = :personId " +
           "OR c.author.id = :personId")
    Page<Task> findTasksConnectedToPerson(@Param("personId") Long personId, Pageable pageable);
    
    @Query("SELECT COALESCE(AVG(t.progressPercentage), 0.0) FROM Task t WHERE t.assignedPerson.id = :personId")
    Double getAverageProgressByAssignedPersonId(@Param("personId") Long personId);
    
    @Query("SELECT COALESCE(AVG(t.progressPercentage), 0.0) FROM Task t WHERE t.assignedTeam.id = :teamId")
    Double getAverageProgressByAssignedTeamId(@Param("teamId") Long teamId);

    // A person's tasks scoped to ONE team, not blended across every team they belong to.
    // A subtask is attributed to a team via its parent (top-level) task's assignedTeam.
    // Used both to fix TeamStatisticsResponse.memberProgresses (previously wrongly averaged
    // a member's tasks org-wide instead of within just this team) and to build a person's
    // per-team stats breakdown (e.g. "50% avg on Auditing App team, 100% on Compliance team").
    @Query("SELECT t FROM Task t WHERE t.assignedPerson.id = :personId AND t.parentTask.assignedTeam.id = :teamId")
    List<Task> findByAssignedPersonIdAndTeamId(@Param("personId") Long personId, @Param("teamId") Long teamId);
    
    // Only ONE List-valued collection can be eagerly join-fetched per query — Hibernate
    // rejects more than that with MultipleBagFetchException ("comments" + "reassignments"
    // together already trips it; adding "subtasks" made it unmissable). comments is the one
    // kept eager since it's what every detail view actually needs; reassignments/subtasks/
    // parentTask lazy-load on access instead — a couple of extra trivial queries per task
    // detail fetch, not a real cost at this scale.
    @EntityGraph(attributePaths = {"comments"})
    @Query("SELECT t FROM Task t WHERE t.id = :id")
    Optional<Task> findWithDetailsById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"comments"})
    @Query("SELECT t FROM Task t WHERE t.taskCode = :taskCode")
    Optional<Task> findWithDetailsByTaskCode(@Param("taskCode") String taskCode);

    // Drives taskCode generation off the highest existing "TSK-NNNN" suffix rather than row
    // count, so deleting a task never causes the next generated code to collide with a
    // surviving one (COUNT(*)-based generation shrinks on delete; MAX(suffix) does not).
    @Query(value = "SELECT COALESCE(MAX(CAST(SUBSTRING(task_code FROM 5) AS INTEGER)), 0) FROM tasks", nativeQuery = true)
    int findMaxTaskCodeSequence();
}
