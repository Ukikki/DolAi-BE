package com.dolai.backend.meeting.service;

import com.dolai.backend.meeting.model.*;
import com.dolai.backend.meeting.repository.MeetingRepository;
import com.dolai.backend.meeting.repository.ParticipantsRepository;
import com.dolai.backend.user.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;

import static com.dolai.backend.meeting.model.Participant.Role.PARTICIPANT;
import static com.dolai.backend.meeting.model.enums.Status.ENDED;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final ParticipantsRepository participantsRepository;

    private final WebClient webClient; // 반드시 사용해야 신뢰 무시됨

    // 1. 새 화상회의 생성
    public MeetingResponseDto createMeeting(MeetingCreateRequestDto request, String userId) {
        if (request.getTitle() == null || request.getTitle().isBlank() || request.getStartTime() == null) {
            throw new IllegalArgumentException("회의 제목(title)과 시작 시간(startTime)은 필수 입력값입니다.");
        }

        // roomId 생성 (timestamp_userId)
        String roomId = System.currentTimeMillis() + "_" + userId;

        // Mediasoup SFU 서버에 방 생성 요청
        try {
            webClient.post()
                    .uri("/api/create-room")
                    .bodyValue(Map.of("roomId", roomId))
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
        } catch (Exception e) {
            throw new RuntimeException("Mediasoup 서버와 연결 실패: " + e.getMessage(), e);
        }

        // 초대 링크 생성
        String inviteUrl = "https://223.194.137.95:3000/sfu/" + roomId;

        // 회의 정보 DB 저장
        Meeting meeting = Meeting.create(request.getTitle(), request.getStartTime(), userId, inviteUrl);
        meetingRepository.save(meeting);

        log.info("회의 생성 완료: {}", meeting);

        return new MeetingResponseDto(meeting.getId(), meeting.getTitle(), meeting.getStartTime(), inviteUrl);
    }

    // 2. 화상회의 참여
    public MeetingResponseDto joinMeeting(User currentUser, JoinMeetingRequestDto request) {
        String inviteUrl = request.getInviteUrl();

        // inviteUrl로 meeting 조회
        Meeting meeting = meetingRepository.findByInviteUrl(inviteUrl)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회의 없음"));

        log.info("✅ 미팅 조회 성공: {}", meeting.getId());

        // 회의 상태 검사
        if (meeting.getStatus() == ENDED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "종료된 회의입니다.");
        }

        // participants가 null일 수 있음 → 이건 엔티티에서 기본값으로 초기화 되어야 함
        if (meeting.getParticipants() == null) {
            log.warn("⚠️ participants 리스트가 null입니다. 초기화합니다.");
            meeting.setParticipants(new ArrayList<>());
        }

        // 이미 등록된 참가자인지 확인
        boolean alreadyJoined = meeting.getParticipants().stream()
                .anyMatch(p -> p.getUser().getId().equals(currentUser.getId()));

        if (!alreadyJoined) {
            Participant participant = Participant.builder()
                    .meeting(meeting)
                    .user(currentUser)
                    .role(PARTICIPANT)
                    .build();
            participantsRepository.save(participant);
        }

        return new MeetingResponseDto(
                meeting.getId(),
                meeting.getTitle(),
                meeting.getStartTime(),
                meeting.getInviteUrl()
        );
    }

    // 3. 화상회의 종료
    public void endMeeting(String meetingId, User user) {
        // 1. 회의 조회
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회의를 찾을 수 없습니다."));

        // 2. 현재 유저가 호스트인지 확인
        if (!meeting.getHostUserId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "회의 주최자만 회의를 종료할 수 있습니다.");
        }

        // 3. 회의 상태 종료로 변경 + 종료 시각 기록
        meeting.setStatus(ENDED);
        meeting.setEndTime(LocalDateTime.now());

        // DB에 회의 정보 업데이트
        meetingRepository.save(meeting);

        log.info("✅ 회의 종료 처리 완료: meetingId={}, host={}", meetingId, user.getEmail());
    }
}
