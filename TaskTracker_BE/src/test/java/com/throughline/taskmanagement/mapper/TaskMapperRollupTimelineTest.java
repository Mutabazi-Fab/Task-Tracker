package com.throughline.taskmanagement.mapper;

import com.throughline.taskmanagement.dto.request.AddCommentRequest;
import com.throughline.taskmanagement.dto.request.CreateSubtaskRequest;
import com.throughline.taskmanagement.dto.request.CreateTaskRequest;
import com.throughline.taskmanagement.dto.response.TaskDetailResponse;
import com.throughline.taskmanagement.dto.response.TaskTimelineResponse;
import com.throughline.taskmanagement.repository.PersonRepository;
import com.throughline.taskmanagement.repository.TaskRepository;
import com.throughline.taskmanagement.repository.TeamRepository;
import com.throughline.taskmanagement.service.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Runs against the real seeded database (same @SpringBootTest pattern as
 *  TaskRepositoryDepartmentScopingTest) — proves the Trend chart on a rollup task (TEAM/DEPARTMENT) is
 *  no longer always empty. */
@SpringBootTest
@Transactional
class TaskMapperRollupTimelineTest {

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
    void rollupTaskGetsAReconstructedTimelineThatEndsAtItsCurrentPercentage() {
        Long taskId = taskRepository.findByTaskCode("TSK-0001")
                .orElseThrow(() -> new IllegalStateException("Seed data missing: TSK-0001"))
                .getId();

        TaskDetailResponse detail = taskService.getTaskById(taskId);
        List<TaskTimelineResponse> timeline = detail.progressTimeline();

        // The whole point: at least 2 points, so TaskProgressSparkline actually renders
        // something instead of returning null.
        assertTrue(timeline.size() >= 2, "Expected a reconstructed multi-point timeline for a "
                + "DEPARTMENT-assigned rollup task with no PROGRESS comments of its own, got: " + timeline);

        // Chronological order.
        for (int i = 1; i < timeline.size(); i++) {
            assertFalse(timeline.get(i).date().isBefore(timeline.get(i - 1).date()),
                    "Timeline must be in chronological order.");
        }

        // The reconstruction's last point must agree with the task's actual, currently-stored
        // rollup percentage — anything else would mean the history and the present disagree.
        TaskTimelineResponse last = timeline.get(timeline.size() - 1);
        assertEquals(detail.progressPercentage(), last.percentage(),
                "The reconstructed timeline's final point should match the task's live rollup percentage.");
    }

    /** TSK-0001 (above) happens to have its own opening-note comment seeded directly via SQL with an
     *  explicit DISCUSSION type, so it never actually exercised the real bug: any task created through the
     *  running app — TaskServiceImpl.addOpeningComment never sets a type, and TaskComment.type defaults to
     *  PROGRESS — gets a spurious PROGRESS comment of its own even when it's a rollup (TEAM/DEPARTMENT)
     *  task. */
    @Test
    void rollupTaskWithItsOwnSpuriousOpeningProgressCommentStillReconstructsFromChildren() {
        Long fabiolaId = idOf("fabiola.ikirezi@ceo.com"); // CEO/Executive
        Long jeanPaulId = idOf("jeanpaul.ndayambaje@example.com"); // Director heading IT
        Long patrickId = idOf("patrick.rugamba@example.com"); // Digital Banking team leader

        TaskDetailResponse departmentTask = taskService.createTask(new CreateTaskRequest(
                "Rollup Timeline Test — Department Task", null, fabiolaId, null, null, 1L,
                LocalDate.now(), LocalDate.now().plusDays(30), null, null, null,
                "Opening note for the department task."));
        TaskDetailResponse implementationTask = taskService.createSubtask(departmentTask.id(), new CreateSubtaskRequest(
                "Rollup Timeline Test — Implementation Task", null, jeanPaulId, null, digitalBankingTeamId(),
                LocalDate.now(), LocalDate.now().plusDays(20), null, null, null,
                "Opening note for the implementation task."));
        TaskDetailResponse leafTask = taskService.createSubtask(implementationTask.id(), new CreateSubtaskRequest(
                "Rollup Timeline Test — Leaf Subtask", null, jeanPaulId, patrickId, null,
                LocalDate.now(), LocalDate.now().plusDays(10), null, null, null,
                "Opening note for the leaf subtask."));

        taskService.addProgressComment(leafTask.id(), new AddCommentRequest(patrickId, 60, "Making progress."));

        TaskDetailResponse detail = taskService.getTaskById(departmentTask.id());
        List<TaskTimelineResponse> timeline = detail.progressTimeline();

        assertTrue(timeline.size() >= 2, "Expected a reconstructed multi-point timeline even though this "
                + "DEPARTMENT task carries its own (spurious) PROGRESS-type opening comment, got: " + timeline);

        TaskTimelineResponse last = timeline.get(timeline.size() - 1);
        assertEquals(detail.progressPercentage(), last.percentage(),
                "The reconstructed timeline's final point should match the task's live rollup percentage.");
    }
}
