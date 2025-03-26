package com.dolai.backend.meeting.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class MeetingResponseDto {
    private String id;
    private String title;
    private LocalDateTime startTime;
    private String inviteUrl;
}
