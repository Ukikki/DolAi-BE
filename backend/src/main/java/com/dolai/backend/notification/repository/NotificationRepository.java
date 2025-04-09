package com.dolai.backend.notification.repository;

import com.dolai.backend.notification.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findAllByReceiverIdOrderByCreatedAtDesc(String receiverId);
}
