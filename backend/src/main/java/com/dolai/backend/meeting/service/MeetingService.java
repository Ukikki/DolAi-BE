package com.dolai.backend.meeting.service;

import com.dolai.backend.meeting.model.*;
import com.dolai.backend.meeting.repository.MeetingRepository;
import com.dolai.backend.meeting.repository.ParticipantsRepository;
import com.dolai.backend.user.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingService {
    private final MediasoupWebSocketClient mediasoupClient;

    private final MeetingRepository meetingRepository;
    private final ParticipantsRepository participantsRepository;

    // 1. 새 화상회의 생성
    public MeetingResponseDto createMeeting(MeetingCreateRequestDto request, String userId) {
        if (request.getTitle() == null || request.getTitle().isBlank() || request.getStartTime() == null) {
            throw new IllegalArgumentException("회의 제목(title)과 시작 시간(startTime)은 필수 입력값입니다.");
        }

        Meeting meeting = Meeting.create(request.getTitle(), request.getStartTime(), userId);
        meetingRepository.save(meeting);

        String inviteUrl = "https://example.com/meetings/" + meeting.getId();
        return new MeetingResponseDto(meeting.getId(), meeting.getTitle(), meeting.getStartTime(), inviteUrl);
    }

    // 2. 화상회의 참여
    public JoinResponseDto joinMeeting(User user, JoinRequestDto requestDto) {
        String meetingId = requestDto.getMeetingId();

        // 1. 회의 찾기
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회의입니다."));

        // 2. 중복 참여 방지
        if (participantsRepository.existsByMeetingIdAndUserId(meetingId.toString(), user.getId())) {
            throw new IllegalStateException("이미 회의에 참가 중입니다.");
        }

        // 3. 기본적으로 참가자로 등록
        Participant participant = Participant.builder()
                .user(user)
                .meeting(meeting)
                .role(Participant.Role.PARTICIPANT)
                .build();
        participantsRepository.save(participant);

        // 4. 성공 응답 반환
        return new JoinResponseDto(
                meeting.getId().toString(),  // meetingId
                user.getId().toString(),     // userId
                "success"                    // status
        );
    }

}
