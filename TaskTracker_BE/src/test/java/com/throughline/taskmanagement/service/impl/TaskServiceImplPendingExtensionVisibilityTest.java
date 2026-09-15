package com.throughline.taskmanagement.service.impl;

import com.throughline.taskmanagement.dto.response.PendingExtensionRequestResponse;
import com.throughline.taskmanagement.model.Person;
import com.throughline.taskmanagement.repository.PersonRepository;
import com.throughline.taskmanagement.service.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs against the real seeded database — a Super Admin's blanket override authority
 * (requireCanDecideDeadline/isDeadlineOverrideTier already let them decide ANY task's
 * extension) was real but invisible: resolveDeadlineDecider never actually resolves to a
 * Super Admin (they don't create tasks in the normal flow), so
 * getPendingExtensionRequests's old per-decider filter always read empty for them. This
 * proves the fix — a Super Admin now sees every pending request org-wide, while an
 * unrelated Director still only sees the ones actually theirs to decide.
 */
@SpringBootTest
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

    @Test
    void superAdminSeesEveryPendingRequestOrgWide() {
        Long superAdminId = idOf("mucyomutabazifabrice@gmail.com");

        List<PendingExtensionRequestResponse> visible = taskService.getPendingExtensionRequests(superAdminId);

        // The seed's one still-undecided request: Vincent Byiringiro's, on TSK-0003 — the
        // Super Admin isn't its resolveDeadlineDecider (Fabiola Ikirezi is), yet it must
        // still show up for them.
        assertTrue(visible.stream().anyMatch(r -> r.taskCode().equals("TSK-0003")),
                "Expected the Super Admin to see TSK-0003's pending request even though they're not its decider.");
    }

    @Test
    void anUnrelatedDirectorStillOnlySeesTheirOwn() {
        // Jean Paul Ndayambaje (IT) has no stake in Vincent's Cybersecurity-department
        // request — unchanged behaviour, still scoped to resolveDeadlineDecider for a
        // plain Director.
        Long unrelatedDirectorId = idOf("jeanpaul.ndayambaje@example.com");

        List<PendingExtensionRequestResponse> visible = taskService.getPendingExtensionRequests(unrelatedDirectorId);

        assertFalse(visible.stream().anyMatch(r -> r.taskCode().equals("TSK-0003")),
                "An unrelated Director should not see a pending request that isn't theirs to decide.");
    }
}
