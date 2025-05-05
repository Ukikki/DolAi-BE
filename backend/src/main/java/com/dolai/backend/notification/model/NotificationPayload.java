package com.dolai.backend.notification.model;

public record NotificationPayload(
        String title,       // 실제 알림 메시지
        String category,    // "친구", "회의", "일정"
        String url          // 접속 할 미팅 경로 등
) {}