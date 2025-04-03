package com.dolai.backend.meeting.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class MeetingResponseDto {
    private String id;
    private String title;
    private LocalDateTime startTime;
    private String inviteUrl;
}
