package com.dolai.backend.meeting.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class MeetingCreateRequestDto {
    private String title;
    private LocalDateTime startTime;
}
