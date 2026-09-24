package com.throughline.taskmanagement.scheduling;

import com.throughline.taskmanagement.enums.TaskStatus;
import com.throughline.taskmanagement.model.Person;
import com.throughline.taskmanagement.model.Task;
import com.throughline.taskmanagement.repository.TaskRepository;
import com.throughline.taskmanagement.repository.TeamMemberRepository;
import com.throughline.taskmanagement.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/** The one background job in this app that runs on its own, not in response to a request — everything
 *  else only ever happens because a client called an endpoint. */
@Component
@RequiredArgsConstructor
public class TaskStalenessJob {

    private static final int STALE_AFTER_DAYS = 7;

    private final TaskRepository taskRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final NotificationService notificationService;

    /** Once a day at 8am server time — a fixed cron rather than a fixed delay, so it fires
     *  at a predictable, human-friendly time instead of drifting relative to server boot. */
    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void flagStalledTasks() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(STALE_AFTER_DAYS);
        List<Task> candidates = taskRepository.findStalledCandidates(TaskStatus.COMPLETED, threshold);

        for (Task task : candidates) {
            Person recipient = resolveRecipient(task);
            if (recipient == null) {
                // A top-level task whose team currently has no Leader set — nobody to tell.
                continue;
            }

            long daysSinceUpdate = Duration.between(task.getUpdatedAt(), LocalDateTime.now()).toDays();
            notificationService.notifyTaskStalled(task, recipient, daysSinceUpdate);

            task.setStaleAlertSentAt(LocalDateTime.now());
            taskRepository.save(task);
        }
    }

    private Person resolveRecipient(Task task) {
        if (task.getAssignedPerson() != null) {
            return task.getAssignedPerson();
        }
        if (task.getAssignedTeam() != null) {
            return teamMemberRepository.findByTeamIdAndIsLeaderTrue(task.getAssignedTeam().getId())
                    .map(tm -> tm.getPerson())
                    .orElse(null);
        }
        if (task.getAssignedDepartment() != null) {
            return task.getAssignedDepartment().getHeadDirector();
        }
        return null;
    }
}
