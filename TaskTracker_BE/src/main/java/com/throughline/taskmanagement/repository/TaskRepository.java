package com.throughline.taskmanagement.repository;

import com.throughline.taskmanagement.enums.Role;
import com.throughline.taskmanagement.enums.TaskSeverity;
import com.throughline.taskmanagement.enums.TaskStatus;
import com.throughline.taskmanagement.model.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    /** A Member's visible task set: assigned to them or their team, plus the ancestor chain (parent,
     *  grandparent) so they can also see the Department/implementation task their work was carved out of. */
    String VISIBLE_TO_PERSON_OR_ANCESTOR =
            "(t.assignedPerson.id = :personId "
            + "OR t.assignedTeam.id IN (SELECT tm.team.id FROM TeamMember tm WHERE tm.person.id = :personId) "
            + "OR t.id IN (SELECT s.parentTask.id FROM Task s WHERE s.parentTask IS NOT NULL AND "
            + "(s.assignedPerson.id = :personId OR s.assignedTeam.id IN (SELECT tm2.team.id FROM TeamMember tm2 WHERE tm2.person.id = :personId))) "
            + "OR t.id IN (SELECT s.parentTask.parentTask.id FROM Task s WHERE s.parentTask IS NOT NULL AND s.parentTask.parentTask IS NOT NULL AND "
            + "(s.assignedPerson.id = :personId OR s.assignedTeam.id IN (SELECT tm3.team.id FROM TeamMember tm3 WHERE tm3.person.id = :personId))))";

    Optional<Task> findByTaskCode(String taskCode);
    
    Page<Task> findByStatus(TaskStatus status, Pageable pageable);

    // Top-level-only status filter for GET /tasks — see findByDepartmentId's own comment
    // for why depth-scoping matters here.
    Page<Task> findByStatusAndParentTaskIsNull(TaskStatus status, Pageable pageable);

    List<Task> findByAssignedPersonId(Long personId);

    // Backs "my tasks" — a Member's scoped GET /tasks view (see VISIBLE_TO_PERSON_OR_ANCESTOR).
    @Query("SELECT t FROM Task t WHERE " + VISIBLE_TO_PERSON_OR_ANCESTOR)
    Page<Task> findVisibleToPerson(@Param("personId") Long personId, Pageable pageable);

    @Query("SELECT t FROM Task t WHERE " + VISIBLE_TO_PERSON_OR_ANCESTOR + " AND t.status = :status")
    Page<Task> findVisibleToPersonAndStatus(@Param("personId") Long personId, @Param("status") TaskStatus status, Pageable pageable);

    List<Task> findByAssignedTeamId(Long teamId);
    
    List<Task> findByAssignedById(Long personId);

    List<Task> findByParentTaskId(Long parentTaskId);

    Page<Task> findByParentTaskIsNull(Pageable pageable);

    // Backs the Director Dashboard's default view: only the top-level tasks THIS Director
    // created. assignedBy doubles as "creator" for top-level tasks.
    Page<Task> findByParentTaskIsNullAndAssignedById(Long assignedById, Pageable pageable);

    // Director-tier department scoping for GET /tasks and /tasks/search — a plain Director only sees tasks
    // belonging to their own department (derived per assignee type: a DEPARTMENT task's own department, a
    // TEAM task's team's department, or an INDIVIDUAL task's assignee's department — exactly one is ever
    // set).
    @Query("SELECT t FROM Task t "
            + "LEFT JOIN t.assignedDepartment dept "
            + "LEFT JOIN t.assignedTeam team LEFT JOIN team.department teamDept "
            + "LEFT JOIN t.assignedPerson person LEFT JOIN person.department personDept "
            + "WHERE (dept.id = :departmentId OR teamDept.id = :departmentId OR personDept.id = :departmentId) "
            + "AND t.parentTask IS NULL")
    Page<Task> findByDepartmentId(@Param("departmentId") Long departmentId, Pageable pageable);

    @Query("SELECT t FROM Task t "
            + "LEFT JOIN t.assignedDepartment dept "
            + "LEFT JOIN t.assignedTeam team LEFT JOIN team.department teamDept "
            + "LEFT JOIN t.assignedPerson person LEFT JOIN person.department personDept "
            + "WHERE (dept.id = :departmentId OR teamDept.id = :departmentId OR personDept.id = :departmentId) "
            + "AND t.status = :status AND t.parentTask IS NULL")
    Page<Task> findByDepartmentIdAndStatus(@Param("departmentId") Long departmentId,
                                            @Param("status") TaskStatus status, Pageable pageable);

    // Same department scope/LEFT-JOIN reasoning as findByDepartmentId, applied to search.
    @Query(value = "SELECT t FROM Task t "
                  + "LEFT JOIN t.assignedDepartment dept "
                  + "LEFT JOIN t.assignedTeam team LEFT JOIN team.department teamDept "
                  + "LEFT JOIN t.assignedPerson person LEFT JOIN person.department personDept "
                  + "WHERE (dept.id = :departmentId OR teamDept.id = :departmentId OR personDept.id = :departmentId) AND "
                  + "(LOWER(t.taskCode) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(t.title) LIKE LOWER(CONCAT('%', :q, '%')))",
           countQuery = "SELECT COUNT(t) FROM Task t "
                  + "LEFT JOIN t.assignedDepartment dept "
                  + "LEFT JOIN t.assignedTeam team LEFT JOIN team.department teamDept "
                  + "LEFT JOIN t.assignedPerson person LEFT JOIN person.department personDept "
                  + "WHERE (dept.id = :departmentId OR teamDept.id = :departmentId OR personDept.id = :departmentId) AND "
                  + "(LOWER(t.taskCode) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(t.title) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<Task> searchByDepartmentId(@Param("q") String q, @Param("departmentId") Long departmentId, Pageable pageable);

    // Unpaged variant of findByDepartmentId, for DashboardServiceImpl.buildDepartmentHealth
    // — needs every top-level task in one shot to aggregate in Java.
    @Query("SELECT t FROM Task t "
            + "LEFT JOIN t.assignedDepartment dept "
            + "LEFT JOIN t.assignedTeam team LEFT JOIN team.department teamDept "
            + "LEFT JOIN t.assignedPerson person LEFT JOIN person.department personDept "
            + "WHERE (dept.id = :departmentId OR teamDept.id = :departmentId OR personDept.id = :departmentId) "
            + "AND t.parentTask IS NULL")
    List<Task> findByDepartmentId(@Param("departmentId") Long departmentId);

    // Backs the Executive Dashboard's task grid: not "every top-level task", but the two things worth an
    // Executive's attention regardless of depth — anything CRITICAL, and anything an Executive/Super Admin
    // personally assigned.
    @Query("SELECT t FROM Task t WHERE t.severity = :severity OR t.assignedBy.role IN :executiveRoles")
    Page<Task> findBySeverityOrAssignedByRoleIn(
            @Param("severity") TaskSeverity severity,
            @Param("executiveRoles") List<Role> executiveRoles,
            Pageable pageable);

    // The department-scoped equivalent of findBySeverityOrAssignedByRoleIn, for the Director Dashboard's
    // "Critical & CEO-assigned" panel — this Director's own department, any depth, matching either filter.
    @Query("SELECT t FROM Task t "
            + "LEFT JOIN t.assignedDepartment dept "
            + "LEFT JOIN t.assignedTeam team LEFT JOIN team.department teamDept "
            + "LEFT JOIN t.assignedPerson person LEFT JOIN person.department personDept "
            + "WHERE (dept.id = :departmentId OR teamDept.id = :departmentId OR personDept.id = :departmentId) "
            + "AND (t.severity IN :severities OR t.assignedBy.role IN :executiveRoles)")
    Page<Task> findByDepartmentIdAndSeverityInOrAssignedByRoleIn(
            @Param("departmentId") Long departmentId,
            @Param("severities") List<TaskSeverity> severities,
            @Param("executiveRoles") List<Role> executiveRoles,
            Pageable pageable);

    long countByStatus(TaskStatus status);

    // Executive Dashboard's overdue KPI tile — top-level only, meant to reconcile with the
    // sum of each department's own overdueCount.
    long countByDeadlineBeforeAndStatusNotAndParentTaskIsNull(LocalDate date, TaskStatus status);

    // Executive Dashboard's "critical, not complete" KPI tile — not parentTask-scoped, same
    // reasoning as findBySeverityOrAssignedByRoleIn.
    long countBySeverityAndStatusNot(TaskSeverity severity, TaskStatus status);

    // Unpaginated — used internally by DashboardService.globalSearch (a bounded endpoint).
    @Query("SELECT t FROM Task t WHERE LOWER(t.taskCode) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(t.title) LIKE LOWER(CONCAT('%', :q, '%'))")
    List<Task> search(@Param("q") String q);

    // Backs the dedicated GET /tasks/search endpoint.
    @Query(value = "SELECT t FROM Task t WHERE LOWER(t.taskCode) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(t.title) LIKE LOWER(CONCAT('%', :q, '%'))",
           countQuery = "SELECT COUNT(t) FROM Task t WHERE LOWER(t.taskCode) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(t.title) LIKE LOWER(CONCAT('%', :q, '%'))")
    Page<Task> search(@Param("q") String q, Pageable pageable);

    // Same search, scoped to what's visible to this person (see findVisibleToPerson).
    @Query(value = "SELECT t FROM Task t WHERE " + VISIBLE_TO_PERSON_OR_ANCESTOR + " AND "
                  + "(LOWER(t.taskCode) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(t.title) LIKE LOWER(CONCAT('%', :q, '%')))",
           countQuery = "SELECT COUNT(t) FROM Task t WHERE " + VISIBLE_TO_PERSON_OR_ANCESTOR + " AND "
                  + "(LOWER(t.taskCode) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(t.title) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<Task> searchVisibleToPerson(@Param("q") String q, @Param("personId") Long personId, Pageable pageable);

    // Used by PersonService.getPersonStatistics for an aggregate over ALL connected tasks —
    // pagination would silently under-count there.
    @Query("SELECT DISTINCT t FROM Task t " +
           "LEFT JOIN t.reassignments r " +
           "LEFT JOIN t.comments c " +
           "WHERE t.assignedPerson.id = :personId " +
           "OR t.assignedTeam.id IN (SELECT tm.team.id FROM TeamMember tm WHERE tm.person.id = :personId) " +
           "OR r.fromPerson.id = :personId " +
           "OR c.author.id = :personId")
    List<Task> findTasksConnectedToPerson(@Param("personId") Long personId);

    // Backs GET /people/{id}/tasks. Explicit countQuery — Spring Data's derived one can't
    // safely replicate COUNT(DISTINCT ...) over this query's joins.
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

    // A person's tasks scoped to ONE team, not blended across every team they belong to — a subtask is
    // attributed via its parent's assignedTeam.
    @Query("SELECT t FROM Task t WHERE t.assignedPerson.id = :personId AND t.parentTask.assignedTeam.id = :teamId")
    List<Task> findByAssignedPersonIdAndTeamId(@Param("personId") Long personId, @Param("teamId") Long teamId);
    
    // Only one List-valued collection can be eagerly join-fetched per query (Hibernate
    // throws MultipleBagFetchException otherwise). comments is the one kept eager since
    // every detail view needs it; reassignments/subtasks/parentTask lazy-load instead.
    @EntityGraph(attributePaths = {"comments"})
    @Query("SELECT t FROM Task t WHERE t.id = :id")
    Optional<Task> findWithDetailsById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"comments"})
    @Query("SELECT t FROM Task t WHERE t.taskCode = :taskCode")
    Optional<Task> findWithDetailsByTaskCode(@Param("taskCode") String taskCode);

    // Drives taskCode generation off the highest existing "TSK-NNNN" suffix, not row count
    // — so deleting a task never causes the next code to collide with a surviving one.
    @Query(value = "SELECT COALESCE(MAX(CAST(SUBSTRING(task_code FROM 5) AS INTEGER)), 0) FROM tasks", nativeQuery = true)
    int findMaxTaskCodeSequence();

    // Backs TaskStalenessJob: not finished, no real progress update since :threshold, and
    // not already flagged (staleAlertSentAt clears the moment progress genuinely moves).
    @Query("SELECT t FROM Task t WHERE t.status <> :completedStatus AND t.updatedAt < :threshold "
            + "AND t.staleAlertSentAt IS NULL")
    List<Task> findStalledCandidates(@Param("completedStatus") TaskStatus completedStatus,
                                      @Param("threshold") LocalDateTime threshold);
}
