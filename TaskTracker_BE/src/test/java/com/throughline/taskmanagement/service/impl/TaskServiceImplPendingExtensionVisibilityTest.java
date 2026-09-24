package com.throughline.taskmanagement.service.impl;

import com.throughline.taskmanagement.dto.request.CreateTaskRequest;
import com.throughline.taskmanagement.dto.request.RequestDeadlineExtensionRequest;
import com.throughline.taskmanagement.dto.response.PendingExtensionRequestResponse;
import com.throughline.taskmanagement.dto.response.TaskDetailResponse;
import com.throughline.taskmanagement.repository.PersonRepository;
import com.throughline.taskmanagement.service.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs against the real database inside a transaction that is rolled back. A Super Admin's
 * blanket override authority (requireCanDecideDeadline/isDeadlineOverrideTier already let
 * them decide ANY task's extension) was real but invisible: resolveDeadlineDecider never
 * resolves to a Super Admin (they don't create tasks in the normal flow), so
 * getPendingExtensionRequests's old per-decider filter always read empty for them. This
 * proves the fix — a Super Admin sees every pending request org-wide, while an unrelated
 * Director still only sees the ones actually theirs to decide.
 *
 * Builds its own throwaway request (Vincent, Cybersecurity's Director, assigns Delphine an
 * individual task and she asks for more time) instead of relying on a particular seeded
 * task code, so it holds no matter how the org was seeded.
 */
@SpringBootTest
@Transactional
class TaskServiceImplPendingExtensionVisibilityTest {

    @Autowired
    private TaskService taskService;

    @Autowired
    private PersonRepository personRepository;

    private Long idOf(String email) {
        return personRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Seed data missing: " + email))
                .getId();
    }

    /** Returns the code of a fresh task whose pending extension request belongs to Vincent. */
    private String createPendingRequestDecidedByVincent() {
        Long vincentId = idOf("vincent.byiringiro@example.com");
        Long delphineId = idOf("delphine.mutesi@example.com");

        TaskDetailResponse task = taskService.createTask(new CreateTaskRequest(
                "Pending Request Visibility Test", null, vincentId, null, delphineId, null,
                LocalDate.now(), LocalDate.now().plusDays(10), null, null, null,
                "Opening note for the visibility test."));
        taskService.requestDeadlineExtension(task.id(), new RequestDeadlineExtensionRequest(
                LocalDate.now().plusDays(20), "Need more time for the visibility test.", delphineId));
        return task.taskCode();
    }

    @Test
    void superAdminSeesEveryPendingRequestOrgWide() {
        String code = createPendingRequestDecidedByVincent();
        Long superAdminId = idOf("mucyomutabazifabrice@gmail.com");

        List<PendingExtensionRequestResponse> visible = taskService.getPendingExtensionRequests(superAdminId);

        // The Super Admin isn't this request's resolveDeadlineDecider (Vincent is), yet it
        // must still show up for them.
        assertTrue(visible.stream().anyMatch(r -> r.taskCode().equals(code)),
                "Expected the Super Admin to see the pending request even though they're not its decider.");
    }

    @Test
    void anUnrelatedDirectorStillOnlySeesTheirOwn() {
        String code = createPendingRequestDecidedByVincent();
        Long unrelatedDirectorId = idOf("jeanpaul.ndayambaje@example.com");
        Long vincentId = idOf("vincent.byiringiro@example.com");

        // Jean Paul Ndayambaje (IT) has no stake in a Cybersecurity-department request —
        // still scoped to resolveDeadlineDecider for a plain Director.
        assertFalse(taskService.getPendingExtensionRequests(unrelatedDirectorId).stream()
                        .anyMatch(r -> r.taskCode().equals(code)),
                "An unrelated Director should not see a pending request that isn't theirs to decide.");
        assertTrue(taskService.getPendingExtensionRequests(vincentId).stream()
                        .anyMatch(r -> r.taskCode().equals(code)),
                "The Director who set the task must see the request.");
    }
}
