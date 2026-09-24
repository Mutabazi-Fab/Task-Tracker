package com.throughline.taskmanagement.access;

import com.throughline.taskmanagement.dto.request.CreateIncidentRequest;
import com.throughline.taskmanagement.dto.request.CreateTaskRequest;
import com.throughline.taskmanagement.dto.request.GrantAccessRequest;
import com.throughline.taskmanagement.dto.response.IncidentDetailResponse;
import com.throughline.taskmanagement.dto.response.TaskDetailResponse;
import com.throughline.taskmanagement.enums.AccessResourceType;
import com.throughline.taskmanagement.enums.IncidentCategory;
import com.throughline.taskmanagement.exception.ForbiddenActionException;
import com.throughline.taskmanagement.exception.InvalidAssignmentException;
import com.throughline.taskmanagement.repository.PersonRepository;
import com.throughline.taskmanagement.service.AccessGrantService;
import com.throughline.taskmanagement.service.IncidentService;
import com.throughline.taskmanagement.service.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Real database, rolled back afterwards. Vincent (Cybersecurity's Director) reports an Information
 * Technology incident and assigns Delphine (Cybersecurity) a task. Jean Paul directs IT, Immaculee directs
 * Human Capital, Sandrine is an ordinary Cybersecurity member, Fabiola is the CEO.
 */
@SpringBootTest
@Transactional
class AccessGrantVisibilityTest {

    @Autowired
    private IncidentService incidentService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private AccessGrantService accessGrantService;

    @Autowired
    private TaskAccessPolicy taskAccessPolicy;

    @Autowired
    private PersonRepository personRepository;

    private Long idOf(String email) {
        return personRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Seed data missing: " + email))
                .getId();
    }

    private IncidentDetailResponse itIncidentReportedByVincent() {
        LocalDate today = LocalDate.now();
        return incidentService.createIncident(new CreateIncidentRequest(
                today, null, today, today, "Information Technology", null, IncidentCategory.ICT_SYSTEMS, null,
                "Access visibility test", null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, idOf("vincent.byiringiro@example.com"), null, null));
    }

    private TaskDetailResponse taskForDelphine() {
        return taskService.createTask(new CreateTaskRequest(
                "Access visibility test", null, idOf("vincent.byiringiro@example.com"), null,
                idOf("delphine.mutesi@example.com"), null, LocalDate.now(), LocalDate.now().plusDays(10),
                null, null, null, "Opening note."));
    }

    private boolean listsIncident(String viewerEmail, String incidentCode) {
        return incidentService.getAllIncidents(null, null, null, null, null, null, null,
                        idOf(viewerEmail), PageRequest.of(0, 200))
                .getContent().stream().anyMatch(i -> i.incidentCode().equals(incidentCode));
    }

    private void grant(String granterEmail, AccessResourceType type, Long resourceId, String granteeEmail) {
        accessGrantService.grant(new GrantAccessRequest(type, resourceId, idOf(granteeEmail), "Needed for review.", idOf(granterEmail)));
    }

    @Test
    void incidentsAreVisibleOnlyToTheirOwnDepartmentTheReporterAndExecutives() {
        IncidentDetailResponse incident = itIncidentReportedByVincent();

        assertDoesNotThrow(() -> incidentService.getIncidentById(incident.id(), idOf("jeanpaul.ndayambaje@example.com")));
        assertDoesNotThrow(() -> incidentService.getIncidentById(incident.id(), idOf("vincent.byiringiro@example.com")));
        assertDoesNotThrow(() -> incidentService.getIncidentById(incident.id(), idOf("fabiola.ikirezi@ceo.com")));
        assertDoesNotThrow(() -> incidentService.getIncidentById(incident.id(), idOf("mucyomutabazifabrice@gmail.com")));

        assertThrows(ForbiddenActionException.class,
                () -> incidentService.getIncidentById(incident.id(), idOf("immaculee.uwimana@example.com")));
        assertThrows(ForbiddenActionException.class,
                () -> incidentService.getIncidentById(incident.id(), idOf("sandrine.nyiraneza@example.com")));

        assertTrue(listsIncident("jeanpaul.ndayambaje@example.com", incident.incidentCode()));
        assertFalse(listsIncident("immaculee.uwimana@example.com", incident.incidentCode()));
        assertFalse(listsIncident("sandrine.nyiraneza@example.com", incident.incidentCode()));
    }

    @Test
    void grantingAnIncidentLetsThatOnePersonSeeItAndRevokingTakesItBack() {
        IncidentDetailResponse incident = itIncidentReportedByVincent();
        Long sandrine = idOf("sandrine.nyiraneza@example.com");

        grant("mucyomutabazifabrice@gmail.com", AccessResourceType.INCIDENT, incident.id(), "sandrine.nyiraneza@example.com");

        assertDoesNotThrow(() -> incidentService.getIncidentById(incident.id(), sandrine));
        assertTrue(accessGrantService.hasActiveGrant(sandrine, AccessResourceType.INCIDENT, incident.id()));
        // Only Sandrine — someone else outside the department still can't.
        assertThrows(ForbiddenActionException.class,
                () -> incidentService.getIncidentById(incident.id(), idOf("immaculee.uwimana@example.com")));

        Long grantId = accessGrantService.listForResource(AccessResourceType.INCIDENT, incident.id(),
                idOf("fabiola.ikirezi@ceo.com")).get(0).id();
        accessGrantService.revoke(grantId, idOf("fabiola.ikirezi@ceo.com"));

        assertThrows(ForbiddenActionException.class, () -> incidentService.getIncidentById(incident.id(), sandrine));
    }

    @Test
    void aTaskIsPrivateToItsDepartmentUntilSharedAndStaysSharedUntilRevoked() {
        TaskDetailResponse task = taskForDelphine();
        Long jeanPaul = idOf("jeanpaul.ndayambaje@example.com");

        assertTrue(taskAccessPolicy.canView(task.id(), idOf("delphine.mutesi@example.com")));
        assertTrue(taskAccessPolicy.canView(task.id(), idOf("vincent.byiringiro@example.com")));
        assertTrue(taskAccessPolicy.canView(task.id(), idOf("fabiola.ikirezi@ceo.com")));
        assertFalse(taskAccessPolicy.canView(task.id(), jeanPaul));
        assertFalse(taskAccessPolicy.canView(task.id(), idOf("sandrine.nyiraneza@example.com")));

        grant("fabiola.ikirezi@ceo.com", AccessResourceType.TASK, task.id(), "jeanpaul.ndayambaje@example.com");
        assertTrue(taskAccessPolicy.canView(task.id(), jeanPaul));

        Long grantId = accessGrantService.listForResource(AccessResourceType.TASK, task.id(),
                idOf("fabiola.ikirezi@ceo.com")).get(0).id();
        accessGrantService.revoke(grantId, idOf("fabiola.ikirezi@ceo.com"));
        assertFalse(taskAccessPolicy.canView(task.id(), jeanPaul));
    }

    @Test
    void aDirectorMayShareOnlyItemsFromTheirOwnDepartment() {
        TaskDetailResponse cyberTask = taskForDelphine();
        IncidentDetailResponse itIncident = itIncidentReportedByVincent();
        Long sandrine = idOf("sandrine.nyiraneza@example.com");

        // Vincent directs Cybersecurity, so he can share Delphine's task, but not an IT incident.
        assertTrue(accessGrantService.canManage(idOf("vincent.byiringiro@example.com"), AccessResourceType.TASK, cyberTask.id()));
        assertFalse(accessGrantService.canManage(idOf("vincent.byiringiro@example.com"), AccessResourceType.INCIDENT, itIncident.id()));
        // Jean Paul directs IT: the opposite.
        assertTrue(accessGrantService.canManage(idOf("jeanpaul.ndayambaje@example.com"), AccessResourceType.INCIDENT, itIncident.id()));
        assertFalse(accessGrantService.canManage(idOf("jeanpaul.ndayambaje@example.com"), AccessResourceType.TASK, cyberTask.id()));

        assertDoesNotThrow(() -> grant("vincent.byiringiro@example.com", AccessResourceType.TASK, cyberTask.id(), "jeanpaul.ndayambaje@example.com"));
        assertThrows(ForbiddenActionException.class,
                () -> grant("vincent.byiringiro@example.com", AccessResourceType.INCIDENT, itIncident.id(), "sandrine.nyiraneza@example.com"));
        assertThrows(ForbiddenActionException.class,
                () -> grant("jeanpaul.ndayambaje@example.com", AccessResourceType.TASK, cyberTask.id(), "sandrine.nyiraneza@example.com"));
        // An ordinary member can't share anything.
        assertFalse(accessGrantService.canManage(sandrine, AccessResourceType.TASK, cyberTask.id()));
        assertThrows(ForbiddenActionException.class,
                () -> grant("sandrine.nyiraneza@example.com", AccessResourceType.TASK, cyberTask.id(), "jeanpaul.ndayambaje@example.com"));
    }

    @Test
    void sharedItemsAppearInThePersonsOwnLists() {
        TaskDetailResponse cyberTask = taskForDelphine();
        IncidentDetailResponse itIncident = itIncidentReportedByVincent();
        Long sandrine = idOf("sandrine.nyiraneza@example.com");
        Long jeanPaul = idOf("jeanpaul.ndayambaje@example.com");
        Long jeanPaulDepartment = personRepository.findById(jeanPaul).orElseThrow().getDepartment().getId();

        boolean before = taskService.getAllTasks(null, sandrine, null, sandrine, PageRequest.of(0, 500))
                .getContent().stream().anyMatch(t -> t.id().equals(cyberTask.id()));
        assertFalse(before);

        grant("fabiola.ikirezi@ceo.com", AccessResourceType.TASK, cyberTask.id(), "sandrine.nyiraneza@example.com");
        grant("fabiola.ikirezi@ceo.com", AccessResourceType.TASK, cyberTask.id(), "jeanpaul.ndayambaje@example.com");
        grant("fabiola.ikirezi@ceo.com", AccessResourceType.INCIDENT, itIncident.id(), "sandrine.nyiraneza@example.com");

        // A Member's "my tasks" list, and a Director's department list, now include the shared task.
        assertTrue(taskService.getAllTasks(null, sandrine, null, sandrine, PageRequest.of(0, 500))
                .getContent().stream().anyMatch(t -> t.id().equals(cyberTask.id())));
        assertTrue(taskService.getAllTasks(null, null, jeanPaulDepartment, jeanPaul, PageRequest.of(0, 500))
                .getContent().stream().anyMatch(t -> t.id().equals(cyberTask.id())));
        assertTrue(listsIncident("sandrine.nyiraneza@example.com", itIncident.incidentCode()));
    }
}
