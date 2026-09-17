package com.throughline.taskmanagement.service.impl;

import com.throughline.taskmanagement.dto.request.CreateDepartmentRequest;
import com.throughline.taskmanagement.dto.response.DepartmentActivityResponse;
import com.throughline.taskmanagement.dto.response.DepartmentResponse;
import com.throughline.taskmanagement.exception.ForbiddenActionException;
import com.throughline.taskmanagement.exception.InvalidAssignmentException;
import com.throughline.taskmanagement.repository.DepartmentRepository;
import com.throughline.taskmanagement.repository.PersonRepository;
import com.throughline.taskmanagement.service.DepartmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs against the real seeded database, wrapped in @Transactional so the throwaway
 * department it creates rolls back at the end.
 */
@SpringBootTest
@Transactional
class DepartmentServiceImplDeleteTest {

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    private Long idOf(String email) {
        return personRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Seed data missing: " + email))
                .getId();
    }

    @Test
    void anExecutiveMayDeleteAnEmptyDepartmentButNotOneThatStillHasTeams() {
        Long fabiolaId = idOf("fabiola.ikirezi@ceo.com");
        Long jeanPaulId = idOf("jeanpaul.ndayambaje@example.com"); // a real Director, valid head

        DepartmentResponse created = departmentService.createDepartment(
                new CreateDepartmentRequest("Delete Test Department", jeanPaulId, fabiolaId));

        // Empty (no teams, no people assigned to it) — should succeed.
        departmentService.deleteDepartment(created.id(), fabiolaId);
        assertTrue(departmentRepository.findById(created.id()).isEmpty());

        // Information Technology has real teams in the seed data — must be refused rather
        // than silently cascading away its teams and their people.
        Long itDepartmentId = departmentRepository.findAll().stream()
                .filter(d -> d.getName().equals("Information Technology"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Seed data missing: Information Technology"))
                .getId();
        assertThrows(InvalidAssignmentException.class, () -> departmentService.deleteDepartment(itDepartmentId, fabiolaId));
    }

    @Test
    void aPlainDirectorMayNotDeleteADepartmentEvenTheOneTheyHead() {
        Long jeanPaulId = idOf("jeanpaul.ndayambaje@example.com"); // heads Information Technology
        Long itDepartmentId = departmentRepository.findAll().stream()
                .filter(d -> d.getName().equals("Information Technology"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Seed data missing: Information Technology"))
                .getId();

        assertThrows(ForbiddenActionException.class, () -> departmentService.deleteDepartment(itDepartmentId, jeanPaulId));
    }

    @Test
    void deletingADepartmentRecordsItOnTheActivityLog() {
        Long fabiolaId = idOf("fabiola.ikirezi@ceo.com");
        Long jeanPaulId = idOf("jeanpaul.ndayambaje@example.com");

        DepartmentResponse created = departmentService.createDepartment(
                new CreateDepartmentRequest("Activity Log Test Department", jeanPaulId, fabiolaId));
        departmentService.deleteDepartment(created.id(), fabiolaId);

        DepartmentActivityResponse entry = departmentService
                .getDepartmentActivity(fabiolaId, PageRequest.of(0, 20))
                .getContent()
                .stream()
                .filter(a -> a.departmentName().equals("Activity Log Test Department"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No activity entry was written for the deleted department"));

        String fabiolaFullName = personRepository.findById(fabiolaId).orElseThrow().getFullName();
        assertEquals(fabiolaFullName, entry.performedByName());
    }
}
