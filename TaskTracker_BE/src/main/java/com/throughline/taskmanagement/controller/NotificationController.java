package com.throughline.taskmanagement.controller;

import com.throughline.taskmanagement.dto.response.NotificationResponse;
import com.throughline.taskmanagement.security.CurrentPersonResolver;
import com.throughline.taskmanagement.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Notifications are inherently personal, so personId is never accepted from the client —
 * it's always the caller's own real, logged-in identity (CurrentPersonResolver), regardless
 * of role. Nobody, not even a Super Admin, can read someone else's notification inbox by
 * passing a different id.
 */
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
}
