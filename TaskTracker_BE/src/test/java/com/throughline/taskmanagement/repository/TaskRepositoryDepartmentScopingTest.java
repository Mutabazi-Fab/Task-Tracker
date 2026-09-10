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
 * now guards against it coming back.
 */
@SpringBootTest
class TaskRepositoryDepartmentScopingTest {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Test
    void findByDepartmentId_returnsTasksAcrossAllThreeAssigneeShapes() {
        Long itDepartmentId = departmentRepository.findAll().stream()
                .filter(d -> d.getName().equals("Information Technology"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Seed data missing: Information Technology department"))
                .getId();

        List<Task> results = taskRepository.findByDepartmentId(itDepartmentId, Pageable.unpaged()).getContent();

        // IT has all three assignee shapes represented in the seed: a DEPARTMENT-assigned
        // top-level task, TEAM-assigned implementation tasks, and INDIVIDUAL-assigned leaf
        // subtasks — the bug this guards against returned zero rows regardless of shape.
        assertFalse(results.isEmpty(), "Expected at least one IT-department task, got none — "
                + "the department-scoping join is dropping rows again.");
        assertTrue(results.stream().anyMatch(t -> t.getAssignedDepartment() != null),
                "Expected a DEPARTMENT-assigned IT task (TSK-0001) to be included.");
        assertTrue(results.stream().anyMatch(t -> t.getAssignedTeam() != null),
                "Expected a TEAM-assigned IT task (e.g. TSK-0009) to be included.");
        assertTrue(results.stream().anyMatch(t -> t.getAssignedPerson() != null),
                "Expected an INDIVIDUAL-assigned IT task (e.g. TSK-0017) to be included.");
    }

    @Test
    void findByDepartmentIdAndStatus_stillFindsTheCompletedTask() {
        Long itDepartmentId = departmentRepository.findAll().stream()
                .filter(d -> d.getName().equals("Information Technology"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Seed data missing: Information Technology department"))
                .getId();

        Page<Task> completed = taskRepository.findByDepartmentIdAndStatus(itDepartmentId, TaskStatus.COMPLETED, Pageable.unpaged());

        // TSK-0018 ("Migrate application servers to new data center") is INDIVIDUAL-assigned
        // to Patrick Rugamba (Information Technology) at 100% — this is the exact row that
        // showed "0" on the dashboard's Completed filter before the join fix.
        assertFalse(completed.isEmpty(), "Expected IT's one COMPLETED task, got none.");
    }
}
