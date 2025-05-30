package com.dolai.backend.notification.model;

import com.dolai.backend.notification.model.enums.Type;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private Type type;

    private String title;

    private String receiverId;

    private String targetUrl;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public static Notification create(Type type, String receiverId, Map<String, String> params, String targetUrl) {
        return Notification.builder()
                .type(type)
                .receiverId(receiverId)
                .title(type.format(params))
                .targetUrl(targetUrl)
                .build();
    }
}
