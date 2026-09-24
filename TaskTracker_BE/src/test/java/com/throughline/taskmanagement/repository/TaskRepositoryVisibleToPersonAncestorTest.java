package com.throughline.taskmanagement.repository;

import com.throughline.taskmanagement.dto.request.CreateSubtaskRequest;
import com.throughline.taskmanagement.dto.request.CreateTaskRequest;
import com.throughline.taskmanagement.dto.response.TaskDetailResponse;
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

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs against the real seeded database, wrapped in @Transactional so the throwaway task
 * chain it creates rolls back at the end. Reproduces the exact scenario reported: a CEO
 * assigns a Department task, a Director carves out an implementation task under it for a
 * team, and a plain team member (not individually named on either task) should be able to
 * see the CEO's original task too, not just the slice handed to their team — see
 * TaskRepository.VISIBLE_TO_PERSON_OR_ANCESTOR.
 */
@SpringBootTest
@Transactional
class TaskRepositoryVisibleToPersonAncestorTest {

    @Autowired
    private TaskService taskService;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TeamRepository teamRepository;

    /** Looked up by name rather than hard-coding id 1 — team ids change whenever the org
     *  is re-seeded, the name doesn't. */
    private Long digitalBankingTeamId() {
        return teamRepository.findByName("Digital Banking")
                .orElseThrow(() -> new IllegalStateException("Seed data missing team: Digital Banking"))
                .getId();
    }

    @Autowired
    private PersonRepository personRepository;

    private Long idOf(String email) {
        return personRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Seed data missing: " + email))
                .getId();
    }

    @Test
    void aTeamMemberSeesTheCeosDepartmentTaskThroughTheirTeamsImplementationTask() {
        Long fabiolaId = idOf("fabiola.ikirezi@ceo.com"); // CEO/Executive
        Long jeanPaulId = idOf("jeanpaul.ndayambaje@example.com"); // Director heading IT
        Long patrickId = idOf("patrick.rugamba@example.com"); // Digital Banking team member, not named on either task

        // Fabiola (CEO) assigns a whole Department a task — the "Data Migration" scenario.
        TaskDetailResponse departmentTask = taskService.createTask(new CreateTaskRequest(
                "Ancestor Visibility Test — Department Task", null, fabiolaId, null, null, 1L,
                LocalDate.now(), LocalDate.now().plusDays(30), null, null, null,
                "Opening note for the department task."));

        // Jean Paul (the IT Director) carves out an implementation task for Digital Banking —
        // Patrick is a member of that team, but neither the department task nor the
        // implementation task names him individually anywhere.
        TaskDetailResponse implementationTask = taskService.createSubtask(departmentTask.id(), new CreateSubtaskRequest(
                "Ancestor Visibility Test — Implementation Task", null, jeanPaulId, null, digitalBankingTeamId(),
                LocalDate.now(), LocalDate.now().plusDays(20), null, null, null,
                "Opening note for the implementation task."));

        Page<Task> visibleToPatrick = taskRepository.findVisibleToPerson(patrickId, Pageable.unpaged());
        List<Long> visibleIds = visibleToPatrick.getContent().stream().map(Task::getId).toList();

        assertTrue(visibleIds.contains(implementationTask.id()),
                "Patrick should see the implementation task assigned to his own team.");
        assertTrue(visibleIds.contains(departmentTask.id()),
                "Patrick should also see the CEO's original Department task it was carved out "
                        + "of, not just the slice handed to his team.");
    }
}
