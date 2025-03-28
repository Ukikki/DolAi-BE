package com.dolai.backend.meeting.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class JoinResponseDto {
    private String meetingId;
    private String userId;
    private String status;
}