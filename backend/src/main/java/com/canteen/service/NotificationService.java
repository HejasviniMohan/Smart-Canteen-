package com.canteen.service;

import com.canteen.entity.Notification;
import com.canteen.entity.User;
import com.canteen.exception.ApiException;
import com.canteen.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repo;

    @Transactional
    public Notification create(User user, String title, String message, String type) {
        return repo.save(Notification.builder()
                .user(user).title(title).message(message).type(type)
                .isRead(false).build());
    }

    @Transactional(readOnly = true)
    public List<Notification> forUser(Long userId) {
        return repo.findTop50ByUserIdOrderByCreatedAtDesc(userId);
    }

    /** Looks up by owner as well as id, so one user cannot touch another's notifications. */
    @Transactional
    public Notification markRead(Long id, Long userId) {
        Notification n = repo.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Notification not found"));
        n.setIsRead(true);
        return n;
    }

    @Transactional
    public int markAllRead(Long userId) {
        return repo.markAllRead(userId);
    }
}
