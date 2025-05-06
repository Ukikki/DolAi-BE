package com.dolai.backend.meeting.model;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class MeetingInviteRequestDto {
    @NotBlank(message = "요청할 대상 userId는 필수입니다.")
    private String targetUserId;
}