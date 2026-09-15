package com.throughline.taskmanagement.service.impl;

import com.throughline.taskmanagement.dto.request.CreateTeamRequest;
import com.throughline.taskmanagement.enums.NotificationType;
import com.throughline.taskmanagement.repository.DepartmentRepository;
import com.throughline.taskmanagement.repository.PersonRepository;
import com.throughline.taskmanagement.service.NotificationService;
import com.throughline.taskmanagement.service.TeamService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs against the real seeded database, wrapped in @Transactional so everything it writes
 * (the new team, its notifications) rolls back at the end of the test instead of
 * permanently polluting the shared dev database the way a plain @SpringBootTest write
 * would — every other real-DB test this session has been read-only; this is the first one
 * that actually creates something, hence the extra care.
 *
 * Proves the new-team broadcast → per-type unread count → mark-category-read cycle end to
 * end: creating a team notifies every OTHER Director-or-above (never the creator), that
 * shows up in getUnreadCountsByType, and marking the category read clears it again.
 */
@SpringBootTest
@Transactional
class NotificationBroadcastTest {

    @Autowired
    private TeamService teamService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    private Long idOf(String email) {
        return personRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Seed data missing: " + email))
                .getId();
    }

    @Test
    void creatingATeamNotifiesOtherDirectorsAndTheBadgeClearsOnMarkRead() {
        Long jeanPaulId = idOf("jeanpaul.ndayambaje@example.com"); // creates the team
        Long emmanuelId = idOf("emmanuel.nkurunziza@example.com"); // an unrelated Director — should be notified
        Long itDepartmentId = departmentRepository.findAll().stream()
                .filter(d -> d.getName().equals("Information Technology"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Seed data missing: Information Technology department"))
                .getId();
        Long ericId = idOf("eric.ndayisenga@example.com");

        teamService.createTeam(new CreateTeamRequest(
                "Notification Broadcast Test Team", jeanPaulId, ericId, List.of(ericId), itDepartmentId));

        Map<NotificationType, Long> emmanuelCounts = notificationService.getUnreadCountsByType(emmanuelId);
        assertTrue(emmanuelCounts.getOrDefault(NotificationType.TEAM_CREATED, 0L) > 0,
                "Expected an unrelated Director to have an unread TEAM_CREATED notification.");

        Map<NotificationType, Long> jeanPaulCounts = notificationService.getUnreadCountsByType(jeanPaulId);
        assertFalse(jeanPaulCounts.getOrDefault(NotificationType.TEAM_CREATED, 0L) > 0,
                "The creator themself should never be notified of their own action.");

        notificationService.markCategoryRead(emmanuelId, List.of(NotificationType.TEAM_CREATED));
        Map<NotificationType, Long> afterMarkRead = notificationService.getUnreadCountsByType(emmanuelId);
        assertFalse(afterMarkRead.getOrDefault(NotificationType.TEAM_CREATED, 0L) > 0,
                "markCategoryRead should have cleared the TEAM_CREATED badge.");
    }
}
