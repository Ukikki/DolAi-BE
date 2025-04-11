package com.dolai.backend.notification.service;

import com.dolai.backend.notification.model.Notification;
import com.dolai.backend.notification.model.NotificationPayload;
import com.dolai.backend.notification.model.NotificationResponseDto;
import com.dolai.backend.notification.model.enums.Type;
import com.dolai.backend.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public void notify(String receiverId, Type type, Map<String, String> params) {
        String title = type.format(params);
        log.info("[알림 전송] to={} type={} title={}", receiverId, type, title);
        Notification notification = Notification.create(type, receiverId, params);
        Notification saved = notificationRepository.save(notification);
        messagingTemplate.convertAndSend(
                "/topic/notifications/" + receiverId,
                new NotificationPayload(saved.getTitle(), type.getCategory())
        );
    }

    public List<NotificationResponseDto> getMyNotifications(String receiverId) {
        return notificationRepository.findByReceiverIdOrderByCreatedAtDesc(receiverId).stream()
                .map(NotificationResponseDto::from)
                .toList();
    }
}
