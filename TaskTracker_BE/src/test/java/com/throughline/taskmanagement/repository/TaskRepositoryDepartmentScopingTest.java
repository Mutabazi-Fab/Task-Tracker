package com.throughline.taskmanagement.repository;

import com.throughline.taskmanagement.dto.request.CreateTaskRequest;
import com.throughline.taskmanagement.dto.response.TaskDetailResponse;
import com.throughline.taskmanagement.enums.Role;
import com.throughline.taskmanagement.enums.TaskSeverity;
import com.throughline.taskmanagement.enums.TaskStatus;
import com.throughline.taskmanagement.model.Task;
import com.throughline.taskmanagement.service.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs against the real seeded database (same @SpringBootTest pattern as
 * ThroughlineApplicationTests), not mocks — a naive JPQL translation of "match any of these
 * three mutually-exclusive nullable associations" silently compiles to ANDed INNER joins
 * and returns zero rows for every department, which no Mockito-based test can ever catch
 * since it never touches Hibernate's actual SQL generation. This is what proved the bug in
 * TaskRepository.findByDepartmentId/findByDepartmentIdAndStatus/searchByDepartmentId and
 * now guards against it coming back — alongside the newer "top-level only" scoping added on
 * top of it (see findByDepartmentId's own doc comment).
 */
@SpringBootTest
@Transactional
class TaskRepositoryDepartmentScopingTest {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private TaskService taskService;

    private Long itDepartmentId() {
        return departmentRepository.findAll().stream()
                .filter(d -> d.getName().equals("Information Technology"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Seed data missing: Information Technology department"))
                .getId();
    }

    private Long idOf(String email) {
        return personRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Seed data missing: " + email))
                .getId();
    }

    @Test
    void findByDepartmentId_returnsOnlyTopLevelTasksAcrossAssigneeShapes() {
        List<Task> results = taskRepository.findByDepartmentId(itDepartmentId(), Pageable.unpaged()).getContent();

        // IT's top-level tasks span at least DEPARTMENT (TSK-0001) and INDIVIDUAL
        // (TSK-0033) shapes in the base seed — the bug this guards against returned zero
        // rows regardless of shape.
        assertFalse(results.isEmpty(), "Expected at least one IT-department top-level task, got none — "
                + "the department-scoping join is dropping rows again.");
        assertTrue(results.stream().anyMatch(t -> t.getAssignedDepartment() != null),
                "Expected a DEPARTMENT-assigned IT task (TSK-0001) to be included.");
        assertTrue(results.stream().anyMatch(t -> t.getAssignedPerson() != null),
                "Expected an INDIVIDUAL-assigned top-level IT task (e.g. TSK-0033) to be included.");
        // The newer half of what this query does: every row is top-level, none of IT's
        // depth-1/2 tasks (e.g. TSK-0009, TSK-0017, TSK-0018) leak in even though they
        // belong to IT too — the Tasks list is meant to read as "what are the
        // initiatives", not every leaf subtask mixed in alongside them.
        assertTrue(results.stream().allMatch(t -> t.getParentTask() == null),
                "Expected every result to be a top-level task (no parent) — a subtask leaked in.");
    }

    @Test
    void findByDepartmentIdAndStatus_findsATopLevelMatchButExcludesACompletedSubtask() {
        Long itDepartmentId = itDepartmentId();

        // TSK-0001 ("Migrate core banking system to new data center") is ONGOING and
        // top-level — proves the join+status filter still finds a real match, not just
        // narrowly avoiding the completed-subtask case below.
        Page<Task> ongoing = taskRepository.findByDepartmentIdAndStatus(itDepartmentId, TaskStatus.ONGOING, Pageable.unpaged());
        assertFalse(ongoing.isEmpty(), "Expected at least one ONGOING top-level IT task, got none.");

        // TSK-0018 ("Migrate application servers to new data center") is INDIVIDUAL-
        // assigned to Patrick Rugamba at 100% COMPLETED — but it's a depth-2 leaf subtask,
        // not top-level, so it must NOT show up here even though it does belong to IT.
        Page<Task> completed = taskRepository.findByDepartmentIdAndStatus(itDepartmentId, TaskStatus.COMPLETED, Pageable.unpaged());
        assertTrue(completed.isEmpty(),
                "Expected no top-level COMPLETED IT task — TSK-0018 is a subtask and should be excluded.");
    }

    @Test
    void findByDepartmentIdAndSeverityInOrAssignedByRoleIn_backsTheDirectorDashboardsCombinedPanel() {
        Long itDepartmentId = itDepartmentId();
        Long fabiolaId = idOf("fabiola.ikirezi@ceo.com"); // CEO/Executive
        Long jeanPaulId = idOf("jeanpaul.ndayambaje@example.com"); // Director heading IT

        // A throwaway CRITICAL, CEO-assigned, DEPARTMENT-assigned task in IT — satisfies both
        // halves of the OR at once, same join shape findByDepartmentId already proves doesn't
        // get silently dropped. Created fresh (wrapped in @Transactional to roll back) rather
        // than depending on a specific pre-existing seed task code, which already broke this
        // test once when that row was later deleted through the running app.
        TaskDetailResponse matching = taskService.createTask(new CreateTaskRequest(
                "Department Scoping Test — Critical CEO Task", null, fabiolaId, null, null, itDepartmentId,
                LocalDate.now(), LocalDate.now().plusDays(30), null, null, TaskSeverity.CRITICAL,
                "Opening note for the combined-panel scoping test."));
        // A throwaway ordinary IT task — no severity, Director-assigned — proves this isn't
        // just "every IT task" ignoring both filter halves.
        TaskDetailResponse nonMatching = taskService.createTask(new CreateTaskRequest(
                "Department Scoping Test — Ordinary Director Task", null, jeanPaulId, 1L, null, null,
                LocalDate.now(), LocalDate.now().plusDays(30), null, null, null,
                "Opening note for the non-matching control task."));

        Page<Task> results = taskRepository.findByDepartmentIdAndSeverityInOrAssignedByRoleIn(
                itDepartmentId, List.of(TaskSeverity.HIGH, TaskSeverity.CRITICAL),
                List.of(Role.EXECUTIVE, Role.SUPER_ADMIN), Pageable.unpaged());

        Set<Long> ids = results.getContent().stream().map(Task::getId).collect(java.util.stream.Collectors.toSet());
        assertTrue(ids.contains(matching.id()),
                "Expected the throwaway CRITICAL/CEO-assigned IT task to be included — the join or filter is dropping rows.");
        assertFalse(ids.contains(nonMatching.id()),
                "A task matching neither filter half leaked into the combined results.");
        assertTrue(results.getContent().stream().allMatch(t ->
                        t.getSeverity() == TaskSeverity.HIGH || t.getSeverity() == TaskSeverity.CRITICAL
                                || t.getAssignedBy().getRole() == Role.EXECUTIVE || t.getAssignedBy().getRole() == Role.SUPER_ADMIN),
                "Every result must actually be HIGH/CRITICAL severity or Executive/Super-Admin-assigned.");
    }
}
