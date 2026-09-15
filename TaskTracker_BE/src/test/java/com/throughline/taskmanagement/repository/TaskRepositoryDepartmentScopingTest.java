package com.throughline.taskmanagement.repository;

import com.throughline.taskmanagement.enums.TaskStatus;
import com.throughline.taskmanagement.model.Task;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

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
class TaskRepositoryDepartmentScopingTest {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    private Long itDepartmentId() {
        return departmentRepository.findAll().stream()
                .filter(d -> d.getName().equals("Information Technology"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Seed data missing: Information Technology department"))
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
}
