package com.dolai.backend.meeting.model;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JoinMeetingRequestDto {
    private String userId;       // optional
    @NotBlank(message = "inviteUrl은 필수 입력값입니다.")
    private String inviteUrl;    // required
    private String meetingId;
}
