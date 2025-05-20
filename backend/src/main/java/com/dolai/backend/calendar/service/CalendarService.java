package com.dolai.backend.calendar.service;

import com.dolai.backend.calendar.model.CalendarCreateRequestDto;
import com.dolai.backend.calendar.model.CalendarDto;
import com.dolai.backend.calendar.model.DayCount;
import com.dolai.backend.calendar.model.MonthlyCalendarDto;
import com.dolai.backend.common.exception.CustomException;
import com.dolai.backend.common.exception.ErrorCode;
import com.dolai.backend.meeting.model.Meeting;
import com.dolai.backend.meeting.model.MeetingResponseDto;
import com.dolai.backend.meeting.model.Participant;
import com.dolai.backend.meeting.model.enums.Status;
import com.dolai.backend.meeting.repository.MeetingRepository;
import com.dolai.backend.meeting.repository.ParticipantsRepository;
import com.dolai.backend.notification.model.enums.Type;
import com.dolai.backend.notification.service.NotificationService;
import com.dolai.backend.user.model.User;
import com.dolai.backend.user.repository.UserRepository;
import io.github.cdimascio.dotenv.Dotenv;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.dolai.backend.meeting.model.Participant.Role.PARTICIPANT;

@Service
@RequiredArgsConstructor
public class CalendarService {

    private final MeetingRepository meetingRepository;
    private final Dotenv dotenv;
    private String publicIp;
    private final UserRepository userRepository;
    private final ParticipantsRepository participantsRepository;
    private final NotificationService notificationService;

    @PostConstruct
    private void init() {
        this.publicIp = dotenv.get("PUBLIC_IP");
    }

    public List<CalendarDto> getMeetingsByDate(LocalDate date, String userId) {
        List<Meeting> meetings = meetingRepository.findMeetingsByParticipant(date, userId);

        return meetings.stream()
                .map(CalendarDto::from)
                .collect(Collectors.toList());
    }


    public MonthlyCalendarDto getMonthlyMeetingDays(int year, int month, String userId) {
        List<Object[]> raw = meetingRepository.countMeetingsByDay(year, month, userId);
        List<DayCount> days = raw.stream()
                .map(row -> new DayCount(((Number) row[0]).intValue(), ((Number) row[1]).longValue()))
                .toList();

        return new MonthlyCalendarDto(year, month, days);
    }


    @Transactional
    public MeetingResponseDto reserveMeetingWithParticipants(CalendarCreateRequestDto request, User hostUser) {
        LocalDateTime startTime = LocalDateTime.parse(request.getStartDateTime());

        // 초대 URL 생성
        String roomId = System.currentTimeMillis() + "_" + hostUser.getId();
        String inviteUrl = "https://" + publicIp + ":3000/sfu/" + roomId;

        // 회의 생성
        Meeting meeting = Meeting.create(request.getTitle(), startTime, hostUser.getId(), inviteUrl, Status.SCHEDULED);
        meetingRepository.save(meeting);

        // ✅ 호스트도 참가자로 저장 (단 한 번만!)
        Participant hostParticipant = Participant.builder()
                .meeting(meeting)
                .user(hostUser)
                .role(Participant.Role.HOST)
                .build();
        participantsRepository.save(hostParticipant);

        // 초대 대상 처리
        for (String email : request.getParticipants()) {
            userRepository.findByEmail(email).ifPresent(invitedUser -> {
                // Participant 등록
                Participant participant = Participant.builder()
                        .meeting(meeting)
                        .user(invitedUser)
                        .role(PARTICIPANT)
                        .build();
                participantsRepository.save(participant);

                notificationService.notify(
                        invitedUser.getId(),
                        Type.MEETING_RESERVED,
                        Map.of(
                                "meetingTitle", meeting.getTitle(),
                                "host", hostUser.getName(),
                                "date", meeting.getStartTime().toString()  // 원하면 포맷팅 가능
                        ),
                        inviteUrl
                );
            });
        }

        return new MeetingResponseDto(
                meeting.getId(),
                meeting.getTitle(),
                meeting.getStartTime(),
                inviteUrl
        );
    }

    @Transactional
    public void cancelReservedMeeting(String meetingId, User user) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEETING_NOT_FOUND));

        // 예약자인지 확인
        if (!meeting.getHostUserId().equals(user.getId())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        // 참가자 조회 (호스트 제외)
        List<Participant> participants = participantsRepository.findByMeetingId(meetingId).stream()
                .filter(p -> p.getRole() != Participant.Role.HOST)
                .toList();

        // 알림 전송
        for (Participant participant : participants) {
            User invitedUser = participant.getUser();

            notificationService.notify(
                    invitedUser.getId(),
                    Type.MEETING_CANCELLED,
                    Map.of(
                            "meetingTitle", meeting.getTitle(),
                            "host", user.getName()
                    ),
                    null  // 취소는 링크 필요 없을 수도 있음
            );
        }

        participantsRepository.deleteAllByMeetingId(meetingId);

        // 삭제 또는 상태 변경
        meetingRepository.delete(meeting);
    }
}
