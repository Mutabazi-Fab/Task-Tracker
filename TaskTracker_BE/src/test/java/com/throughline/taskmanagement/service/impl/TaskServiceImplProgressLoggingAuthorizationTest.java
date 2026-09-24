package com.throughline.taskmanagement.service.impl;

import com.throughline.taskmanagement.dto.request.AddCommentRequest;
import com.throughline.taskmanagement.dto.request.CreateTaskRequest;
import com.throughline.taskmanagement.dto.response.TaskDetailResponse;
import com.throughline.taskmanagement.exception.ForbiddenActionException;
import com.throughline.taskmanagement.repository.PersonRepository;
import com.throughline.taskmanagement.service.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Progress on an individually-assigned task is the assignee's to log. */
@SpringBootTest
@Transactional
class TaskServiceImplProgressLoggingAuthorizationTest {

    @Autowired
    private TaskService taskService;

    @Autowired
    private PersonRepository personRepository;

    private Long idOf(String email) {
        return personRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Seed data missing: " + email))
                .getId();
    }

    private Long taskAssignedToDelphine() {
        TaskDetailResponse task = taskService.createTask(new CreateTaskRequest(
                "Progress Logging Authorization Test", null, idOf("vincent.byiringiro@example.com"), null,
                idOf("delphine.mutesi@example.com"), null, LocalDate.now(), LocalDate.now().plusDays(10),
                null, null, null, "Opening note."));
        return task.id();
    }

    private Long taskAssignedToSandrine() {
        TaskDetailResponse task = taskService.createTask(new CreateTaskRequest(
                "Team Leader Progress Logging Test", null, idOf("vincent.byiringiro@example.com"), null,
                idOf("sandrine.nyiraneza@example.com"), null, LocalDate.now(), LocalDate.now().plusDays(10),
                null, null, null, "Opening note."));
        return task.id();
    }

    @Test
    void theLeaderOfTheAssigneesOwnTeamCanLogProgress() {
        // Innocent Bizimana leads Identity & Access Management, which Sandrine belongs to.
        Long taskId = taskAssignedToSandrine();

        TaskDetailResponse updated = taskService.addProgressComment(taskId,
                new AddCommentRequest(idOf("innocent.bizimana@example.com"), 25, "Logged by the team leader."));

        assertEquals(25, updated.progressPercentage());
    }

    @Test
    void aLeaderOfADifferentTeamCannotLogProgress() {
        // Olivier Habimana leads Settlement Operations (Payments), not Sandrine's team.
        Long taskId = taskAssignedToSandrine();

        assertThrows(ForbiddenActionException.class, () -> taskService.addProgressComment(taskId,
                new AddCommentRequest(idOf("olivier.habimana@example.com"), 25, "Not my team.")));
    }

    @Test
    void theAssigneeCanLogProgress() {
        Long taskId = taskAssignedToDelphine();

        TaskDetailResponse updated = taskService.addProgressComment(taskId,
                new AddCommentRequest(idOf("delphine.mutesi@example.com"), 15, "Started."));

        assertEquals(15, updated.progressPercentage());
    }

    @Test
    void anotherMemberCannotLogProgressOnSomeoneElsesTask() {
        Long taskId = taskAssignedToDelphine();

        assertThrows(ForbiddenActionException.class, () -> taskService.addProgressComment(taskId,
                new AddCommentRequest(idOf("sandrine.nyiraneza@example.com"), 40, "Not mine to log.")));
    }

    @Test
    void directorExecutiveAndSuperAdminCanStillLogOnAnyTask() {
        Long taskId = taskAssignedToDelphine();

        taskService.addProgressComment(taskId, new AddCommentRequest(idOf("vincent.byiringiro@example.com"), 20, "Director."));
        taskService.addProgressComment(taskId, new AddCommentRequest(idOf("fabiola.ikirezi@ceo.com"), 30, "CEO."));
        TaskDetailResponse updated = taskService.addProgressComment(taskId,
                new AddCommentRequest(idOf("mucyomutabazifabrice@gmail.com"), 35, "Super Admin."));

        assertEquals(35, updated.progressPercentage());
    }
}
