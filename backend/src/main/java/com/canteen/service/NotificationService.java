package com.canteen.service;

import com.canteen.entity.Notification;
import com.canteen.entity.User;
import com.canteen.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
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
        return repo.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public void markRead(Long id) {
        repo.findById(id).ifPresent(n -> {
            n.setIsRead(true);
            repo.save(n);
        });
    }
}