package com.dolai.backend.meeting.service;

import com.dolai.backend.common.exception.CustomException;
import com.dolai.backend.common.exception.ErrorCode;
import com.dolai.backend.directory.model.Directory;
import com.dolai.backend.directory.service.DirectoryService;
import com.dolai.backend.document.model.Document;
import com.dolai.backend.document.service.DocumentPlacementService;
import com.dolai.backend.llm.LlmDocumentService;
import com.dolai.backend.meeting.model.*;
import com.dolai.backend.meeting.model.enums.Status;
import com.dolai.backend.meeting.repository.MeetingRepository;
import com.dolai.backend.meeting.repository.ParticipantsRepository;
import com.dolai.backend.notification.model.enums.Type;
import com.dolai.backend.notification.service.NotificationService;
import com.dolai.backend.document.service.DocumentService;
import com.dolai.backend.user.model.User;
import com.dolai.backend.user.repository.UserRepository;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.dolai.backend.meeting.model.Participant.Role.PARTICIPANT;
import static com.dolai.backend.meeting.model.enums.Status.ENDED;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final ParticipantsRepository participantsRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final DocumentService documentService;
    private final DocumentPlacementService documentPlacementService;
    private final LlmDocumentService llmDocumentService;
    private final DirectoryService directoryService;
    private final WebClient mediasoupWebClient;
    private final Dotenv dotenv;

    //    @PostConstruct
//    private void init() {
//        this.publicIp = dotenv.get("PUBLIC_IP");
//    }

    // 1. 새 화상회의 생성
    public MeetingResponseDto createMeeting(MeetingCreateRequestDto request, String userId, Status status) {
        if (request.getTitle() == null || request.getTitle().isBlank() || request.getStartTime() == null) {
            throw new IllegalArgumentException("회의 제목(title)과 시작 시간(startTime)은 필수 입력값입니다.");
        }

        // roomId 생성 (timestamp_userId)
        String roomId = System.currentTimeMillis() + "_" + userId;

        // 초대 링크 생성
        String publicIp = "13.209.37.189";

        String inviteUrl = "https://" + publicIp + ":3000/sfu/" + roomId;

        log.info("초대 링크 생성 완료: {}", inviteUrl);

        // Mediasoup SFU 서버에 방 생성 요청
        if (status == Status.ONGOING) {
            createRoomOnMediasoup(roomId);
        }

        // 회의 정보 DB 저장
        Meeting meeting = Meeting.create(request.getTitle(), request.getStartTime(), userId, inviteUrl, status);
        meetingRepository.save(meeting);

        log.info("회의 생성 완료: {}", meeting);

        return new MeetingResponseDto(meeting.getId(), meeting.getTitle(), meeting.getStartTime(), inviteUrl);
    }

    // 참가자 초대
    public void inviteUserToMeeting(String meetingId, String targetUserId, User hostUser) {
        // 회의 조회
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEETING_NOT_FOUND));

        // 초대 대상 유저 조회
        User invitedUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 알림 전송 + 저장
        notificationService.notify(
                invitedUser.getId(),
                Type.MEETING_INVITE,
                Map.of(
                        "meetingTitle", meeting.getTitle(),
                        "host", hostUser.getName()
                ),
                meeting.getInviteUrl() // ✅ 전체 URL 그대로 보내기
        );
        log.info("✅ {}님에게 회의 초대 알림 전송 완료", invitedUser.getName());
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
            throw new CustomException(ErrorCode.MEETING_ENDED);
        }

        // participants가 null일 수 있음 → 이건 엔티티에서 기본값으로 초기화 되어야 함
        if (meeting.getParticipants() == null) {
            log.warn("⚠️ participants 리스트가 null입니다. 초기화합니다.");
            meeting.setParticipants(new ArrayList<>());
        }

        // 현재 유저가 호스트인지 판단
        boolean isHost = meeting.getHostUserId().equals(currentUser.getId());
        if (meeting.getStatus() == Status.SCHEDULED) {
            if (isHost) {
                String roomId = extractRoomIdFromUrl(meeting.getInviteUrl());
                createRoomOnMediasoup(roomId);
                meeting.setStatus(Status.ONGOING);
                meetingRepository.save(meeting);
                log.info("✅ 호스트 입장: 방 생성하고 상태를 ONGOING으로 변경");
            } else {
                log.warn("❌ 참가자가 회의 시작 전에 입장 시도: userId={}", currentUser.getId());
                throw new CustomException(ErrorCode.MEETING_ALREADY_STARTED); // 호스트가 회의를 시작하지 않았습니다.
            }
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
                .orElseThrow(() -> new CustomException(ErrorCode.MEETING_NOT_FOUND));

        // 2. 현재 유저가 호스트인지 확인
        if (!meeting.getHostUserId().equals(user.getId())) {
            throw new CustomException(ErrorCode.MEETING_HOST_ONLY);  // 회의 주최자만 회의를 종료할 수 있음
        }

        // 3. 회의 상태 종료로 변경 + 종료 시각 기록
        meeting.setStatus(ENDED);
        meeting.setEndTime(LocalDateTime.now());

        // DB에 회의 정보 업데이트
        meetingRepository.save(meeting);

        log.info("✅ 회의 종료 처리 완료: meetingId={}, host={}", meetingId, user.getEmail());

        // 4 회의록, directory, document, documentPlacement 생성
        try {
            createMeetingAssets(meeting, user);
        } catch (Exception e) {
            log.error("❌ 생성 중 오류 발생", e);
        }
    }

    // Mediasoup 방 생성
    private void createRoomOnMediasoup(String roomId) {
        try {
            mediasoupWebClient.post()
                    .uri("/api/create-room")
                    .bodyValue(Map.of("roomId", roomId))
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
        } catch (Exception e) {
            throw new RuntimeException("Mediasoup 서버와 연결 실패: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void createMeetingAssets(Meeting meeting, User user) {
        Map<String, Map<String, String>> docInfo = llmDocumentService.summarizeAndGenerateDoc(meeting.getId());
        Directory sharedDirectory = directoryService.createSharedDirectory(meeting, user);
        // 각 언어별 문서 생성 및 디렉토리에 연결
        for (Map.Entry<String, Map<String, String>> entry : docInfo.entrySet()) {
            Map<String, String> info = entry.getValue();
            String docUrl = info.get("url");    // S3 URL
            String title = info.get("title");   // 해당 언어로 번역된 제목

            // 각 언어별 문서 생성
            Document document = documentService.createDocument(meeting, docUrl, title);

            // 문서를 디렉토리에 연결
            documentPlacementService.linkDocumentToDirectory(document, sharedDirectory, user);
        }
    }

    // inviteUrl에서 roomId 추출
    private String extractRoomIdFromUrl(String inviteUrl) {
        return inviteUrl.substring(inviteUrl.lastIndexOf("/") + 1);
    }

    //최근 3개의 미팅 내역 조회
    // 최근 3개의 미팅 내역 조회
    public List<MeetingResponseDto> getRecentEndedMeetings(User user) {
        List<Meeting> meetings = meetingRepository.findTop3EndedMeetingsByUserId(user.getId());

        return meetings.stream()
                .map(m -> new MeetingResponseDto(
                        m.getId(),
                        m.getTitle(),
                        m.getStartTime(),
                        m.getInviteUrl()
                ))
                .toList();
    }


    // 종료된 전체 미팅 내역 조회
    public List<MeetingResponseDto> getAllEndedMeetings(User user) {
        List<Meeting> meetings = meetingRepository.findAllEndedMeetingsByUserId(user.getId());
        return meetings.stream()
                .map(m -> new MeetingResponseDto(
                        m.getId(),
                        m.getTitle(),
                        m.getStartTime(),
                        m.getInviteUrl()
                ))
                .toList();
    }


}
