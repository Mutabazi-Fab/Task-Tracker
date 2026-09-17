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

    /**
     * A Member's visible task set: assigned to them directly, or to a team they belong to —
     * PLUS the ancestor chain of any such task (its parent, and its parent's parent), so a
     * team member working on a Director's implementation task can also see the CEO's
     * original Department task it was carved out of, not just the slice handed to their own
     * team. Task depth never exceeds 2 (see AssigneeType's own doc comment: 0 = top-level, 1
     * = implementation task, 2 = leaf subtask), so two ancestor hops (parent, grandparent)
     * always covers it — no recursive query needed. Deliberately does NOT expand to siblings
     * or the parent's other children — reachable by navigating INTO the now-visible parent
     * (see SubtasksPanel/the frontend's parent-task breadcrumb link), not surfaced again
     * here as a flat list.
     */
    String VISIBLE_TO_PERSON_OR_ANCESTOR =
            "(t.assignedPerson.id = :personId "
            + "OR t.assignedTeam.id IN (SELECT tm.team.id FROM TeamMember tm WHERE tm.person.id = :personId) "
            + "OR t.id IN (SELECT s.parentTask.id FROM Task s WHERE s.parentTask IS NOT NULL AND "
            + "(s.assignedPerson.id = :personId OR s.assignedTeam.id IN (SELECT tm2.team.id FROM TeamMember tm2 WHERE tm2.person.id = :personId))) "
            + "OR t.id IN (SELECT s.parentTask.parentTask.id FROM Task s WHERE s.parentTask IS NOT NULL AND s.parentTask.parentTask IS NOT NULL AND "
            + "(s.assignedPerson.id = :personId OR s.assignedTeam.id IN (SELECT tm3.team.id FROM TeamMember tm3 WHERE tm3.person.id = :personId))))";

    Optional<Task> findByTaskCode(String taskCode);
    
    Page<Task> findByStatus(TaskStatus status, Pageable pageable);

    // Backs GET /tasks' unfiltered-by-department, status-filtered case, scoped to top-level
    // tasks only — see the parentTaskIsNull reasoning on findByDepartmentId below; this is
    // the same idea for the plain "just a status filter" branch of getAllTasks.
    Page<Task> findByStatusAndParentTaskIsNull(TaskStatus status, Pageable pageable);

    List<Task> findByAssignedPersonId(Long personId);

    // Backs "my tasks" — a Member's scoped view of GET /tasks: everything they're currently
    // responsible for, either assigned to them directly (individual tasks/subtasks) or as a
    // top-level task assigned to a team they belong to, PLUS the ancestor chain of any of
    // that (see VISIBLE_TO_PERSON_OR_ANCESTOR) — not the org's whole task list, and not just
    // their individual work in isolation from their team's or from the initiative it's part of.
    @Query("SELECT t FROM Task t WHERE " + VISIBLE_TO_PERSON_OR_ANCESTOR)
    Page<Task> findVisibleToPerson(@Param("personId") Long personId, Pageable pageable);

    @Query("SELECT t FROM Task t WHERE " + VISIBLE_TO_PERSON_OR_ANCESTOR + " AND t.status = :status")
    Page<Task> findVisibleToPersonAndStatus(@Param("personId") Long personId, @Param("status") TaskStatus status, Pageable pageable);

    List<Task> findByAssignedTeamId(Long teamId);
    
    List<Task> findByAssignedById(Long personId);

    List<Task> findByParentTaskId(Long parentTaskId);

    Page<Task> findByParentTaskIsNull(Pageable pageable);

    // Backs the Director's Dashboard default view: only the top-level tasks THIS Director
    // created, not the whole org's tasks. assignedBy doubles as "creator" for top-level
    // tasks (see TaskServiceImpl.createTask).
    Page<Task> findByParentTaskIsNullAndAssignedById(Long assignedById, Pageable pageable);

    // Backs Director-tier department scoping on GET /tasks and /tasks/search — a plain
    // Director (not Executive/Super Admin, who stay unrestricted) only sees tasks that
    // belong to their own department. "Belongs to" is derived per assignee type, not a
    // stored column: a DEPARTMENT-assigned task's own assignedDepartment, a TEAM-assigned
    // task's team's department, or an INDIVIDUAL-assigned task's assignee's own department
    // — exactly one of these three associations is ever non-null per task (the
    // createTask/createSubtask XOR invariant). Every hop below is an EXPLICIT LEFT JOIN,
    // not path navigation (t.assignedTeam.department.id) — that looks equivalent but isn't:
    // Hibernate compiles a bare path expression through a to-one association as an INNER
    // join, so with three mutually-exclusive nullable associations ANDed together in one
    // FROM clause, at most one of the three joins could ever succeed and EVERY row would
    // get silently dropped, no matter how the OR conditions read. Caught via a real-DB
    // repository test (TaskRepositoryDepartmentScopingTest), not by inspection — it
    // returned zero rows for every department despite matching data existing.
    //
    // t.parentTask IS NULL scopes this to top-level tasks only, same as
    // findByStatusAndParentTaskIsNull/findByParentTaskIsNull below — the org-wide/
    // department-wide Tasks list is meant to read as "what are the initiatives", not every
    // individual leaf subtask mixed in; a subtask's own page already shows it (as a row in
    // its parent's Subtasks panel) without it needing to also appear here. Only reachable
    // when getAllTasks has no assignedPersonId to scope by — a Member's own "My Tasks" is
    // untouched by this (findVisibleToPerson*, below), since their assigned work is very
    // often exactly a leaf subtask, not a top-level task at all.
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

    // Same department scope (and same explicit-LEFT-JOIN reasoning) as findByDepartmentId,
    // applied to search — mirrors searchVisibleToPerson's own explicit countQuery reasoning.
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

    // Same department scope/explicit-LEFT-JOIN reasoning as the paged findByDepartmentId
    // above — an unpaged variant for DashboardServiceImpl.buildDepartmentHealth, which needs
    // every one of a department's top-level tasks in one shot to aggregate in Java, the same
    // way getTeamLeaderboard/getPeopleSummary already do per-team/per-person. Same method
    // name as the paged version, resolved fine since both are explicit @Query (no derivation
    // ambiguity).
    @Query("SELECT t FROM Task t "
            + "LEFT JOIN t.assignedDepartment dept "
            + "LEFT JOIN t.assignedTeam team LEFT JOIN team.department teamDept "
            + "LEFT JOIN t.assignedPerson person LEFT JOIN person.department personDept "
            + "WHERE (dept.id = :departmentId OR teamDept.id = :departmentId OR personDept.id = :departmentId) "
            + "AND t.parentTask IS NULL")
    List<Task> findByDepartmentId(@Param("departmentId") Long departmentId);

    // Backs the Executive Dashboard's task grid: not "every top-level task" any more (see
    // findByParentTaskIsNull, now unused by that view but left in place — depth-based
    // top-level browsing still makes sense elsewhere, e.g. a future "initiatives" filter),
    // but the two things actually worth an Executive's attention regardless of depth —
    // anything flagged CRITICAL, and anything an Executive/Super Admin personally assigned
    // (a Department task today, but not assumed to stay that way forever). Deliberately not
    // depth-scoped: a CRITICAL subtask (Super Admin can set severity at any depth, unlike a
    // plain Director) belongs here just as much as a CRITICAL Department task does.
    @Query("SELECT t FROM Task t WHERE t.severity = :severity OR t.assignedBy.role IN :executiveRoles")
    Page<Task> findBySeverityOrAssignedByRoleIn(
            @Param("severity") TaskSeverity severity,
            @Param("executiveRoles") List<Role> executiveRoles,
            Pageable pageable);

    // Backs the Director Dashboard's "Critical & CEO-assigned" panel — the department-scoped
    // equivalent of findBySeverityOrAssignedByRoleIn above, one combined query rather than
    // two separate panels: this Director's own department (membership, same read-visibility
    // boundary as findByDepartmentId — not the stricter headship check writes use), anything
    // whose severity is in :severities OR whose assignedBy holds one of :executiveRoles, any
    // depth. Not depth-scoped, same reasoning as findBySeverityOrAssignedByRoleIn — a
    // HIGH/CRITICAL subtask, or one an Executive personally assigned, deserves a Director's
    // attention just as much as a top-level task does. Same explicit-LEFT-JOIN department
    // derivation as findByDepartmentId — see that method's comment for why bare path
    // navigation would silently drop every row here too.
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

    // Backs the Executive Dashboard's org-health KPI tile and (implicitly) the
    // department-health roll-up's own overdueCount column — top-level only (parentTask IS
    // NULL), same scoping as findByDepartmentId, so the KPI tile's total is meant to
    // reconcile with the sum of the department table's own overdueCount values.
    long countByDeadlineBeforeAndStatusNotAndParentTaskIsNull(LocalDate date, TaskStatus status);

    // Backs the Executive Dashboard's "critical, not complete" KPI tile. Deliberately NOT
    // parentTask-scoped — mirrors findBySeverityOrAssignedByRoleIn's precedent that a
    // CRITICAL subtask matters exactly as much as a CRITICAL Department task.
    long countBySeverityAndStatusNot(TaskSeverity severity, TaskStatus status);

    // List-returning: used internally by DashboardService.globalSearch, which stays
    // unpaginated per the "leave the low-risk/bounded dashboard endpoints as-is" decision.
    @Query("SELECT t FROM Task t WHERE LOWER(t.taskCode) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(t.title) LIKE LOWER(CONCAT('%', :q, '%'))")
    List<Task> search(@Param("q") String q);

    // Page-returning: backs the dedicated GET /tasks/search endpoint, which grows with the
    // org's total task count.
    @Query(value = "SELECT t FROM Task t WHERE LOWER(t.taskCode) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(t.title) LIKE LOWER(CONCAT('%', :q, '%'))",
           countQuery = "SELECT COUNT(t) FROM Task t WHERE LOWER(t.taskCode) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(t.title) LIKE LOWER(CONCAT('%', :q, '%'))")
    Page<Task> search(@Param("q") String q, Pageable pageable);

    // Same search, scoped to what's visible to this person (see findVisibleToPerson,
    // including the same ancestor-chain expansion) — a Member's search shouldn't surface
    // tasks that aren't theirs, their team's, or an ancestor of either, any more than the
    // plain list should.
    @Query(value = "SELECT t FROM Task t WHERE " + VISIBLE_TO_PERSON_OR_ANCESTOR + " AND "
                  + "(LOWER(t.taskCode) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(t.title) LIKE LOWER(CONCAT('%', :q, '%')))",
           countQuery = "SELECT COUNT(t) FROM Task t WHERE " + VISIBLE_TO_PERSON_OR_ANCESTOR + " AND "
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

    // Backs TaskStalenessJob: not finished, hasn't had a real progress update since
    // :threshold, and hasn't already been flagged for this particular stale stretch
    // (staleAlertSentAt is cleared the moment progress genuinely moves again — see
    // TaskServiceImpl.addProgressComment/recalculateParentRollup).
    @Query("SELECT t FROM Task t WHERE t.status <> :completedStatus AND t.updatedAt < :threshold "
            + "AND t.staleAlertSentAt IS NULL")
    List<Task> findStalledCandidates(@Param("completedStatus") TaskStatus completedStatus,
                                      @Param("threshold") LocalDateTime threshold);
}
