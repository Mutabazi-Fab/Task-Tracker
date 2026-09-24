package com.throughline.taskmanagement.service.impl;

import com.throughline.taskmanagement.dto.request.CreateTaskRequest;
import com.throughline.taskmanagement.dto.request.RequestDeadlineExtensionRequest;
import com.throughline.taskmanagement.dto.response.TaskDetailResponse;
import com.throughline.taskmanagement.enums.TaskActivityAction;
import com.throughline.taskmanagement.repository.PersonRepository;
import com.throughline.taskmanagement.repository.TaskActivityRepository;
import com.throughline.taskmanagement.repository.TaskCommentRepository;
import com.throughline.taskmanagement.repository.TaskDeadlineExtensionRequestRepository;
import com.throughline.taskmanagement.repository.TaskDocumentRepository;
import com.throughline.taskmanagement.repository.TaskRepository;
import com.throughline.taskmanagement.service.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Runs against the real seeded database, wrapped in @Transactional so everything it writes (the
 *  throwaway task, its opening comment and extension request, the DELETED activity row) rolls back at
 *  the end instead of polluting the shared dev database. */
@SpringBootTest
@Transactional
class TaskServiceImplDeleteCascadeTest {

    @Autowired
    private TaskService taskService;

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskCommentRepository taskCommentRepository;

    @Autowired
    private TaskDeadlineExtensionRequestRepository taskDeadlineExtensionRequestRepository;

    @Autowired
    private TaskActivityRepository taskActivityRepository;

    @Autowired
    private TaskDocumentRepository taskDocumentRepository;

    private Long idOf(String email) {
        return personRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Seed data missing: " + email))
                .getId();
    }

    @Test
    void deletingATaskCascadesItsCommentsAndExtensionRequestsButLeavesTheActivityLogEntry() {
        Long jeanPaulId = idOf("jeanpaul.ndayambaje@example.com"); // Director — creates, requests, deletes
        Long ericId = idOf("eric.ndayisenga@example.com"); // individual assignee

        TaskDetailResponse created = taskService.createTask(new CreateTaskRequest(
                "Delete Cascade Test Task", null, jeanPaulId, null, ericId, null,
                LocalDate.now(), LocalDate.now().plusDays(10), null, null, null,
                "Opening note for the cascade-delete test."));
        Long taskId = created.id();
        String taskCode = created.taskCode();

        // Jean Paul requests as the override tier (Director), not as the task's own
        // accountable person — irrelevant to what's being proven here, just the simplest
        // valid way to get a real pending extension request row onto this task.
        taskService.requestDeadlineExtension(taskId, new RequestDeadlineExtensionRequest(
                LocalDate.now().plusDays(20), "Testing cascade delete.", jeanPaulId));
        taskService.addDocument(taskId, "notes.txt", "text/plain",
                "cascade delete test file".getBytes(StandardCharsets.UTF_8), jeanPaulId);

        assertFalse(taskCommentRepository.findByTaskIdOrderByCreatedAtAsc(taskId).isEmpty(),
                "Sanity check: the opening comment should exist before deletion.");
        assertTrue(
                taskDeadlineExtensionRequestRepository.findByTaskIdOrderByRequestedAtDesc(taskId, Pageable.unpaged())
                        .hasContent(),
                "Sanity check: the extension request should exist before deletion.");
        assertFalse(taskDocumentRepository.findByTaskIdOrderByUploadedAtAsc(taskId).isEmpty(),
                "Sanity check: the document should exist before deletion.");

        taskService.deleteTask(taskId, jeanPaulId);

        assertTrue(taskRepository.findById(taskId).isEmpty(), "The task itself should be gone.");
        assertTrue(taskCommentRepository.findByTaskIdOrderByCreatedAtAsc(taskId).isEmpty(),
                "Its comments should have been cascade-deleted, not left orphaned.");
        assertFalse(
                taskDeadlineExtensionRequestRepository.findByTaskIdOrderByRequestedAtDesc(taskId, Pageable.unpaged())
                        .hasContent(),
                "Its extension requests should have been cascade-deleted, not left orphaned.");
        assertTrue(taskDocumentRepository.findByTaskIdOrderByUploadedAtAsc(taskId).isEmpty(),
                "Its documents should have been cascade-deleted from the database, not left orphaned.");

        boolean hasDeletedActivityEntry = taskActivityRepository.findAll().stream()
                .anyMatch(activity -> activity.getTaskCode().equals(taskCode)
                        && activity.getAction() == TaskActivityAction.DELETED);
        assertTrue(hasDeletedActivityEntry,
                "The Activity log's own DELETED entry for this task should survive — it holds no FK to Task.");
    }
}
