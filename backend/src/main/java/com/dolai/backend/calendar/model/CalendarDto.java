package com.dolai.backend.calendar.model;

import com.dolai.backend.meeting.model.Meeting;
import com.dolai.backend.meeting.model.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CalendarDto {

    private String meetingId;
    private String title;
    private String startTime;   // ISO 8601 문자열로 변환
    private List<String> participants;
    private String inviteUrl;
    private Status status;

    public static CalendarDto from(Meeting meeting) {
        return CalendarDto.builder()
                .meetingId(meeting.getId())
                .title(meeting.getTitle())
                .startTime(meeting.getStartTime().toString()) // LocalDateTime을 문자열로
                .participants(meeting.getParticipants().stream()
                        .map(participant -> participant.getUser().getId()) // userId 가져오기
                        .collect(Collectors.toList()))
                .inviteUrl(meeting.getInviteUrl())
                .status(meeting.getStatus())
                .build();
    }
}