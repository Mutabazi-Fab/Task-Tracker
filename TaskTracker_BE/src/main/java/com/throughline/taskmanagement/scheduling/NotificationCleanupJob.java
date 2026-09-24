package com.throughline.taskmanagement.scheduling;

import com.throughline.taskmanagement.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** Keeps the notifications table from growing forever. */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationCleanupJob {

    static final int READ_RETENTION_DAYS = 90;
    static final int MAX_RETENTION_DAYS = 365;

    private final NotificationRepository notificationRepository;

    /** 3am server time, well away from TaskStalenessJob (8am) so the two never overlap. */
    @Scheduled(cron = "0 0 3 * * *")
    public void scheduledCleanup() {
        cleanUp(LocalDateTime.now());
    }

    /** Split from the scheduled entry point so a test (or an admin tool) can pass "now"
     *  explicitly instead of depending on the wall clock. Returns how many rows were removed. */
    @Transactional
    public int cleanUp(LocalDateTime now) {
        int tooOld = notificationRepository.deleteAllCreatedBefore(now.minusDays(MAX_RETENTION_DAYS));
        int oldRead = notificationRepository.deleteReadCreatedBefore(now.minusDays(READ_RETENTION_DAYS));
        int total = tooOld + oldRead;
        if (total > 0) {
            log.info("Notification cleanup removed {} rows ({} older than {} days, {} read and older than {} days).",
                    total, tooOld, MAX_RETENTION_DAYS, oldRead, READ_RETENTION_DAYS);
        }
        return total;
    }
}
