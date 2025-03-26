package com.dolai.backend.meeting.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "meeting", uniqueConstraints = {
        @UniqueConstraint(columnNames = "invite_url")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Meeting {

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
    private String hostUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public static Meeting create(String title, LocalDateTime startTime, String hostUserId) {
        Meeting meeting = new Meeting();
        meeting.id = UUID.randomUUID().toString();
        meeting.title = title;
        meeting.startTime = startTime;
        meeting.hostUserId = hostUserId;
        meeting.status = Status.SCHEDULED;
        meeting.createdAt = LocalDateTime.now();
        meeting.inviteUrl = "https://example.com/meetings/" + meeting.id;
        return meeting;
    }

    public enum Status {
        SCHEDULED, ONGOING, ENDED
    }
}