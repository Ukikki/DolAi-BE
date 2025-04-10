package com.dolai.backend.notification.model;

public record NotificationPayload(
        String title,
        String type,
        String createdAt
) {}