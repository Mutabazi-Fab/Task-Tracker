package com.throughline.taskmanagement.scheduling;

import com.throughline.taskmanagement.enums.NotificationType;
import com.throughline.taskmanagement.model.Notification;
import com.throughline.taskmanagement.model.Person;
import com.throughline.taskmanagement.repository.NotificationRepository;
import com.throughline.taskmanagement.repository.PersonRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Same pattern as the other repository tests here: real Hibernate + database inside a
 * transaction that is rolled back, so the bulk DELETEs are exercised for real (a mocked
 * repository can't catch a wrong JPQL condition) and nothing is left behind.
 */
@SpringBootTest
@Transactional
class NotificationCleanupJobTest {

    @Autowired
    private NotificationCleanupJob job;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long saveAged(Person recipient, boolean read, int ageDays) {
        Notification n = new Notification();
        n.setRecipient(recipient);
        n.setType(NotificationType.TASK_ASSIGNED);
        n.setMessage("cleanup test");
        n.setRead(read);
        Long id = notificationRepository.saveAndFlush(n).getId();
        // created_at is set by @CreationTimestamp and is not updatable through the entity,
        // so backdate it directly.
        jdbcTemplate.update("UPDATE notifications SET created_at = ? WHERE id = ?",
                Timestamp.valueOf(LocalDateTime.now().minusDays(ageDays)), id);
        return id;
    }

    @Test
    void removesReadOlderThan90DaysAndAnythingOlderThanAYear_keepsEverythingElse() {
        Person someone = personRepository.findAll().get(0);

        Long readRecent = saveAged(someone, true, 30);        // read, young        -> keep
        Long readOld = saveAged(someone, true, 100);          // read, > 90 days    -> delete
        Long unreadOld = saveAged(someone, false, 200);       // unread, < 1 year   -> keep
        Long unreadAncient = saveAged(someone, false, 400);   // unread, > 1 year   -> delete
        Long readAncient = saveAged(someone, true, 400);      // read, > 1 year     -> delete

        job.cleanUp(LocalDateTime.now());

        assertTrue(notificationRepository.existsById(readRecent), "a recent read notification must stay");
        assertTrue(notificationRepository.existsById(unreadOld), "an unread notification under a year old must stay");
        assertFalse(notificationRepository.existsById(readOld), "a read notification older than 90 days must go");
        assertFalse(notificationRepository.existsById(unreadAncient), "anything older than a year must go, even unread");
        assertFalse(notificationRepository.existsById(readAncient), "anything older than a year must go");
    }
}
