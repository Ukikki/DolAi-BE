package com.dolai.backend.meeting.model;

import com.dolai.backend.common.model.BaseTimeEntity;
import com.dolai.backend.todo.model.enums.Status;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "meeting")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Meeting extends BaseTimeEntity {

    @Id
    @Column(columnDefinition = "CHAR(36)")
    private String id;

    @Column(nullable = false)
    private String title;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "invite_url", nullable = false, unique = true)
    private String inviteUrl;

    @Column(name = "host_user_id", nullable = false)
    private String hostUserId;  // String → Long 변경 (BIGINT 대응)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    public static Meeting create(String title, LocalDateTime startTime, String hostUserId) {
        return Meeting.builder()
                .id(UUID.randomUUID().toString())  // UUID 자동 생성
                .title(title)
                .startTime(startTime)
                .hostUserId(hostUserId)
                .inviteUrl("https://example.com/meetings/" + UUID.randomUUID()) // 초대 URL 자동 생성
                .status(Status.PENDING) // 기본 상태: 대기 중
                .build();
    }
}
