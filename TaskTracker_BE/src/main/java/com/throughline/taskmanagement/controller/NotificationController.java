package com.throughline.taskmanagement.controller;

import com.throughline.taskmanagement.dto.response.NotificationResponse;
import com.throughline.taskmanagement.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * personId is passed explicitly (not derived from a JWT) — same pattern as the rest of the
 * app until Phase 7 wires a real "current user" through from the frontend's login.
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<Page<NotificationResponse>> getNotifications(
            @RequestParam Long personId, Pageable pageable) {
        return ResponseEntity.ok(notificationService.getNotifications(personId, pageable));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(
            @PathVariable Long id, @RequestParam Long personId) {
        return ResponseEntity.ok(notificationService.markAsRead(id, personId));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Long> getUnreadCount(@RequestParam Long personId) {
        return ResponseEntity.ok(notificationService.getUnreadCount(personId));
    }
}
