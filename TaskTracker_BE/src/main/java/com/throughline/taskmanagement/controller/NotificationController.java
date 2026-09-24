package com.throughline.taskmanagement.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.throughline.taskmanagement.dto.response.NotificationResponse;
import com.throughline.taskmanagement.enums.NotificationType;
import com.throughline.taskmanagement.security.CurrentPersonResolver;
import com.throughline.taskmanagement.service.NotificationService;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

/** Notifications are inherently personal, so personId is never accepted from the client — it's always
 *  the caller's own real, logged-in identity (CurrentPersonResolver), regardless of role. */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final CurrentPersonResolver currentPersonResolver;

    @GetMapping
    public ResponseEntity<Page<NotificationResponse>> getNotifications(Pageable pageable, Authentication authentication) {
        Long personId = currentPersonResolver.resolveId(authentication);
        return ResponseEntity.ok(notificationService.getNotifications(personId, pageable));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(@PathVariable Long id, Authentication authentication) {
        Long personId = currentPersonResolver.resolveId(authentication);
        return ResponseEntity.ok(notificationService.markAsRead(id, personId));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Long> getUnreadCount(Authentication authentication) {
        Long personId = currentPersonResolver.resolveId(authentication);
        return ResponseEntity.ok(notificationService.getUnreadCount(personId));
    }

    /** Backs the sidebar's per-section badges (Teams/Departments/Activity) — one call
     *  covering every type at once. A type with zero unread simply isn't a key. */
    @GetMapping("/unread-counts-by-type")
    public ResponseEntity<Map<NotificationType, Long>> getUnreadCountsByType(Authentication authentication) {
        Long personId = currentPersonResolver.resolveId(authentication);
        return ResponseEntity.ok(notificationService.getUnreadCountsByType(personId));
    }

    /** Called when the viewer opens the page a badge points at, to clear it — e.g.
     *  ?types=TEAM_CREATED when the Teams page mounts. */
    @PutMapping("/mark-category-read")
    public ResponseEntity<Void> markCategoryRead(@RequestParam List<NotificationType> types, Authentication authentication) {
        Long personId = currentPersonResolver.resolveId(authentication);
        notificationService.markCategoryRead(personId, types);
        return ResponseEntity.noContent().build();
    }
}
