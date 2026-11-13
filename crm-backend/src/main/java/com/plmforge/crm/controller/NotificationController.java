package com.plmforge.crm.controller;

import com.plmforge.crm.dto.response.ApiResponse;
import com.plmforge.crm.dto.response.NotificationResponse;
import com.plmforge.crm.security.UserPrincipal;
import com.plmforge.crm.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "Per-user notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(summary = "View all of my notifications")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> notifications(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                notificationService.getNotifications(principal.getId()),
                "Notifications retrieved"));
    }

    @GetMapping("/unread")
    @Operation(summary = "View my unread notifications")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> unread(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                notificationService.getUnreadNotifications(principal.getId()),
                "Unread notifications retrieved"));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark one of my notifications as read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        NotificationResponse notification = notificationService.markAsRead(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(notification, "Notification marked as read"));
    }
}
