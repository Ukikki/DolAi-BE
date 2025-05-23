package com.dolai.backend.meeting.service;

import com.dolai.backend.common.exception.CustomException;
import com.dolai.backend.common.exception.ErrorCode;
import com.dolai.backend.directory.model.Directory;
import com.dolai.backend.directory.repository.DirectoryRepository;
import com.dolai.backend.directory.service.DirectoryService;
import com.dolai.backend.document.model.Document;
import com.dolai.backend.document.model.enums.FileType;
import com.dolai.backend.document.service.DocumentPlacementService;
import com.dolai.backend.llm.LlmDocumentService;
import com.dolai.backend.meeting.model.*;
import com.dolai.backend.meeting.model.enums.Status;
import com.dolai.backend.meeting.repository.MeetingRepository;
import com.dolai.backend.meeting.repository.ParticipantsRepository;
import com.dolai.backend.notification.model.enums.Type;
import com.dolai.backend.notification.service.NotificationService;
import com.dolai.backend.document.service.DocumentService;
import com.dolai.backend.stt_log.service.STTLogService;
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
    private final DirectoryRepository directoryRepository;
    private final STTLogService sttLogService;
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
        String publicIp = "3.34.92.187";

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
        // ✅ 호스트를 참가자로 등록 (자동으로)
        User hostUser = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Participant hostParticipant = Participant.builder()
                .meeting(meeting)
                .user(hostUser)
                .role(PARTICIPANT) // 혹은 HOST로 따로 지정하고 싶으면 Role.HOST로
                .build();

        participantsRepository.save(hostParticipant);
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
                    .uri("/create-room")
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
            String lang = entry.getKey();         // "ko", "en", "zh"
            Map<String, String> info = entry.getValue();
            String docUrl = info.get("url");      // S3 URL
            String title = info.get("title");     // 해당 언어로 번역된 제목
            String summary = info.get("summary"); // 한 줄 요약

            // summary 파라미터 추가
            Document document = documentService.createDocument(meeting, docUrl, title, summary, user);
            documentPlacementService.linkDocumentToDirectory(document, sharedDirectory, user);
        }

        String titleKo = docInfo.get("ko").get("title");
        String titleEn = docInfo.get("en").get("title");
        String titleZh = docInfo.get("zh").get("title");

        // 요약 정보도 함께 가져오기
        String summaryKo = docInfo.get("ko").get("summary");
        String summaryEn = docInfo.get("en").get("summary");
        String summaryZh = docInfo.get("zh").get("summary");

        Map<String, String> titleMap = Map.of("ko", titleKo, "en", titleEn, "zh", titleZh);

        // STT 텍스트 문서들 생성 및 저장 (ko, en, zh)
        Map<String, String> sttTxtUrls = sttLogService.generateTxtFilesAndUpload(meeting.getId(), titleMap);
        for (Map.Entry<String, String> entry : sttTxtUrls.entrySet()) {
            String lang = entry.getKey();     // "ko", "en", "zh"
            String txtUrl = entry.getValue(); // S3 URL
            String baseTitle = switch (lang) {
                case "ko" -> titleKo;
                case "en" -> titleEn;
                case "zh" -> titleZh;
                default -> "회의";
            };

            String label = switch (lang) {
                case "ko" -> "자막";
                case "en" -> "Transcript";
                case "zh" -> "字幕";
                default -> "Transcript";
            };

            String title = baseTitle + "_" + label;

            // 언어별 요약 선택
            String summary = switch (lang) {
                case "ko" -> summaryKo;
                case "en" -> summaryEn;
                case "zh" -> summaryZh;
                default -> summaryKo;
            };

            // summary 파라미터 추가
            Document txtDoc = documentService.createDocument(meeting, txtUrl, title, summary, user);
            documentPlacementService.linkDocumentToDirectory(txtDoc, sharedDirectory, user);
        }
    }

    // inviteUrl에서 roomId 추출
    private String extractRoomIdFromUrl(String inviteUrl) {
        return inviteUrl.substring(inviteUrl.lastIndexOf("/") + 1);
    }

    public List<MeetingListResponseDto> getRecentEndedMeetings(User user) {
        Pageable top4 = PageRequest.of(0, 4);  // ← 여기서 4개 제한
        List<Meeting> meetings = meetingRepository.findTopEndedMeetingsByUserId(user.getId(), top4);

        return meetings.stream()
                .map(meeting -> {
                    // 🔧 변경됨: 예외를 던지지 않고 Optional로 받음
                    return directoryRepository.findByMeetingId(meeting.getId())
                            .map(directory -> new MeetingListResponseDto(
                                    meeting.getId(),
                                    meeting.getTitle(),
                                    meeting.getStartTime(),
                                    meeting.getInviteUrl(),
                                    directory.getId()
                            ))
                            .orElse(null); // 🔧 변경됨: 디렉터리가 없으면 null 반환
                })
                .filter(java.util.Objects::nonNull) // 🔧 변경됨: null 필터링
                .toList();
    }

    // 종료된 전체 미팅 내역 조회
    public List<MeetingListResponseDto> getAllEndedMeetings(User user) {
        List<Meeting> meetings = meetingRepository.findAllEndedMeetingsByUserId(user.getId());
        return meetings.stream()
                .map(meeting -> {
                    Directory directory = directoryRepository.findByMeetingId(meeting.getId())
                            .orElseThrow(() -> new CustomException(ErrorCode.DIRECTORY_NOT_FOUND));

                    return new MeetingListResponseDto(
                            meeting.getId(),
                            meeting.getTitle(),
                            meeting.getStartTime(),
                            meeting.getInviteUrl(),
                            directory.getId()
                    );
                })
                .toList();
    }
}
