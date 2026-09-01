package com.tms.service;

import com.tms.dto.response.NotificationResponse;
import com.tms.entity.Notification;
import com.tms.enums.NotificationType;
import com.tms.exception.ResourceNotFoundException;
import com.tms.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepo;
    private final SimpMessagingTemplate messagingTemplate;

    @Async
    public void createAndPush(UUID userId, String username, String title, String message, NotificationType type, String link) {
        try {
            Notification n = Notification.builder()
                    .userId(userId).title(title).message(message).type(type).link(link).build();
            notificationRepo.save(n);
            NotificationResponse response = toResponse(n);
            messagingTemplate.convertAndSendToUser(username, "/queue/notifications", response);
            log.debug("Notification pushed to user {}: {}", username, title);
        } catch (Exception e) {
            log.warn("Failed to push notification to {}: {}", username, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(UUID userId, int page, int size) {
        return notificationRepo.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return notificationRepo.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public NotificationResponse markAsRead(UUID notificationId) {
        Notification n = notificationRepo.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));
        n.setRead(true);
        return toResponse(notificationRepo.save(n));
    }

    @Transactional
    public void markAllRead(UUID userId) {
        notificationRepo.markAllReadByUserId(userId);
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId()).userId(n.getUserId()).title(n.getTitle())
                .message(n.getMessage()).type(n.getType()).read(n.getRead())
                .link(n.getLink()).createdAt(n.getCreatedAt()).build();
    }
}

