package com.dolai.backend.notification.model;

public record NotificationPayload(
        String title,       // 실제 알림 메시지
        String category    // "친구", "회의", "일정"
) {}