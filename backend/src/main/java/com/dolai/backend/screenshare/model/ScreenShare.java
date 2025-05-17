package com.dolai.backend.screenshare.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScreenShare {
    @Id
    private String meetingId;
    private String userId; // 화면 공유 사람

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private boolean active;
}