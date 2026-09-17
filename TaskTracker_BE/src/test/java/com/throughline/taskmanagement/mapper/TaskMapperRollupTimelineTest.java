package com.throughline.taskmanagement.mapper;

import com.throughline.taskmanagement.dto.response.TaskDetailResponse;
import com.throughline.taskmanagement.dto.response.TaskTimelineResponse;
import com.throughline.taskmanagement.repository.TaskRepository;
import com.throughline.taskmanagement.service.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs against the real seeded database (same @SpringBootTest pattern as
 * TaskRepositoryDepartmentScopingTest) — proves the Trend chart on a rollup task
 * (TEAM/DEPARTMENT) is no longer always empty. TSK-0001 is a DEPARTMENT task with no
 * PROGRESS comments of its own; before TaskMapper.buildRollupTimeline existed, its
 * progressTimeline was always `List.of()`, which is exactly what TaskProgressSparkline
 * renders as nothing (points.length < 2 -> null) — the bug the user reported.
 */
@SpringBootTest
class TaskMapperRollupTimelineTest {

    @Autowired
    private TaskService taskService;

    @Autowired
    private TaskRepository taskRepository;

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

    /** TSK-0001 (above) happens to have its own opening-note comment seeded directly via SQL
     *  with an explicit DISCUSSION type, so it never actually exercised the real bug: any
     *  task created through the running app — TaskServiceImpl.addOpeningComment never sets
     *  a type, and TaskComment.type defaults to PROGRESS — gets a spurious PROGRESS comment
     *  of its own even when it's a rollup (TEAM/DEPARTMENT) task. The old code treated "has
     *  its own PROGRESS comment" as "is individually tracked", so that one spurious
     *  0%-at-creation comment short-circuited straight past the real reconstruction, leaving
     *  the Trend chart stuck showing a single 0% point forever. TSK-0034 (a DEPARTMENT task,
     *  → TSK-0035 TEAM implementation task → TSK-0036 INDIVIDUAL leaf, all created live
     *  through the app during this session, not seeded) reproduces this exactly. */
    @Test
    void rollupTaskWithItsOwnSpuriousOpeningProgressCommentStillReconstructsFromChildren() {
        Long taskId = taskRepository.findByTaskCode("TSK-0034")
                .orElseThrow(() -> new IllegalStateException("Seed data missing: TSK-0034"))
                .getId();

        TaskDetailResponse detail = taskService.getTaskById(taskId);
        List<TaskTimelineResponse> timeline = detail.progressTimeline();

        assertTrue(timeline.size() >= 2, "Expected a reconstructed multi-point timeline even though this "
                + "DEPARTMENT task carries its own (spurious) PROGRESS-type opening comment, got: " + timeline);

        TaskTimelineResponse last = timeline.get(timeline.size() - 1);
        assertEquals(detail.progressPercentage(), last.percentage(),
                "The reconstructed timeline's final point should match the task's live rollup percentage.");
    }
}
