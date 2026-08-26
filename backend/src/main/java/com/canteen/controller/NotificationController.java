package com.canteen.controller;

import com.canteen.entity.Notification;
import com.canteen.repository.UserRepository;
import com.canteen.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "User notifications")
public class NotificationController {

    private final NotificationService service;
    private final UserRepository userRepo;

    @GetMapping("/me")
    @Operation(summary = "My notifications")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> mine(Authentication auth) {
        Long userId = userRepo.findByEmail(auth.getName())
                .orElseThrow().getId();
        return service.forUser(userId).stream().map(NotificationController::toDto).toList();
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Mark notification as read")
    @Transactional
    public Map<String, Object> markRead(@PathVariable Long id) {
        service.markRead(id);
        return Map.of("id", id, "isRead", true);
    }

    private static Map<String, Object> toDto(Notification n) {
        return Map.of(
                "id", n.getId(),
                "title", n.getTitle(),
                "message", n.getMessage(),
                "type", n.getType(),
                "isRead", n.getIsRead(),
                "createdAt", n.getCreatedAt(),
                "readAt", (Object) (n.getIsRead() ? LocalDateTime.now() : null)
        );
    }
}