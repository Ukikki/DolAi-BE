package com.dolai.backend.meeting.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class JoinRequestDto {

    @NotBlank(message = "meetingId는 필수 입력값입니다.")
    private String meetingId;
}

