package com.canteen.controller;

import com.canteen.entity.Notification;
import com.canteen.exception.ApiException;
import com.canteen.repository.UserRepository;
import com.canteen.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "User notifications")
public class NotificationController {

    public record NotificationResponse(Long id, String title, String message, String type,
                                       Boolean isRead, LocalDateTime createdAt) {
        static NotificationResponse of(Notification n) {
            return new NotificationResponse(n.getId(), n.getTitle(), n.getMessage(), n.getType(),
                    n.getIsRead(), n.getCreatedAt());
        }
    }

    private final NotificationService service;
    private final UserRepository userRepo;

    @GetMapping("/me")
    @Operation(summary = "My latest notifications")
    public List<NotificationResponse> mine(Authentication auth) {
        return service.forUser(userId(auth)).stream().map(NotificationResponse::of).toList();
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Mark one of my notifications as read")
    public NotificationResponse markRead(Authentication auth, @PathVariable Long id) {
        return NotificationResponse.of(service.markRead(id, userId(auth)));
    }

    @PutMapping("/me/read-all")
    @Operation(summary = "Mark all my notifications as read")
    public void markAllRead(Authentication auth) {
        service.markAllRead(userId(auth));
    }

    private Long userId(Authentication auth) {
        return userRepo.findByEmail(auth.getName())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found"))
                .getId();
    }
}
