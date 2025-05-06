package com.dolai.backend.notification.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class NotificationResponseDto {
    private String title;
    private String category;
    private String targetUrl;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    public static NotificationResponseDto from(Notification notification) {
        return new NotificationResponseDto(
                notification.getTitle(),
                notification.getType().getCategory(),
                notification.getTargetUrl(),
                notification.getCreatedAt()
        );
    }
}