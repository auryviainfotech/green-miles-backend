package com.greenmiles.backend.notification;

import com.greenmiles.backend.audit.AuditLogService;
import com.greenmiles.backend.common.ApiResponse;
import com.greenmiles.backend.notification.dto.CreateNotificationRequest;
import com.greenmiles.backend.notification.dto.NotificationResponse;
import com.greenmiles.backend.security.AuthContext;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final AuthContext authContext;
    private final AuditLogService auditLogService;

    public NotificationController(
            NotificationService notificationService, AuthContext authContext, AuditLogService auditLogService) {
        this.notificationService = notificationService;
        this.authContext = authContext;
        this.auditLogService = auditLogService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<NotificationResponse>> create(
            @Valid @RequestBody CreateNotificationRequest request) {
        enforceUserOwnership(request.userId());
        return ResponseEntity.ok(ApiResponse.ok("Notification sent", notificationService.create(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getByUser(@RequestParam Long userId) {
        enforceUserOwnership(userId);
        return ResponseEntity.ok(ApiResponse.ok("Notifications fetched", notificationService.getByUser(userId)));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markRead(@PathVariable Long notificationId) {
        NotificationResponse notification = notificationService.getById(notificationId);
        enforceUserOwnership(notification.userId());
        return ResponseEntity.ok(ApiResponse.ok("Notification marked as read", notificationService.markRead(notificationId)));
    }

    private void enforceUserOwnership(Long targetUserId) {
        if (authContext.isAdmin()) {
            return;
        }
        if (!targetUserId.equals(authContext.currentUserId())) {
            auditLogService.logDeniedAccess(
                    authContext.currentActorIdOrNull(),
                    "NOTIFICATION",
                    null,
                    "User ownership mismatch for userId=" + targetUserId);
            throw new AccessDeniedException("You are not allowed to access this user resource");
        }
    }
}
