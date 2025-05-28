package com.dolai.backend.notification.service;

import com.dolai.backend.meeting.model.Meeting;
import com.dolai.backend.meeting.model.Participant;
import com.dolai.backend.meeting.repository.MeetingRepository;
import com.dolai.backend.notification.model.Notification;
import com.dolai.backend.notification.model.NotificationPayload;
import com.dolai.backend.notification.model.NotificationResponseDto;
import com.dolai.backend.notification.model.enums.Type;
import com.dolai.backend.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final MeetingRepository meetingRepository;

    public void notify(String receiverId, Type type, Map<String, String> params, String targetUrl) {
        String title = type.format(params);
        log.info("[알림 전송] to={} type={} title={}", receiverId, type, title);
        Notification notification = Notification.create(type, receiverId, params, targetUrl);
        Notification saved = notificationRepository.save(notification);
        messagingTemplate.convertAndSend(
                "/topic/notifications/" + receiverId,
                new NotificationPayload(saved.getTitle(), type.getCategory(), saved.getTargetUrl())
        );
    }

    public List<NotificationResponseDto> getMyNotifications(String receiverId) {
        return notificationRepository.findByReceiverIdOrderByCreatedAtDesc(receiverId).stream()
                .map(NotificationResponseDto::from)
                .toList();
    }
    public void notifyDolAi(String meetingId, Type type, Map<String, String> params, String targetUrl) {
        String title = type.format(params);
        log.info("[회의 중 참가자에게 실시간 알림 + 저장] meetingId={} type={} title={}", meetingId, type, title);

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new IllegalArgumentException("해당 회의를 찾을 수 없습니다."));

        List<Participant> participants = meeting.getParticipants();

        for (Participant participant : participants) {
            String receiverId = participant.getUser().getId().toString();

            Notification notification = Notification.create(type, receiverId, params, targetUrl);
            Notification saved = notificationRepository.save(notification);

            messagingTemplate.convertAndSend(
                    "/topic/notifications/dolai/" + receiverId,
                    new NotificationPayload(saved.getTitle(), type.getCategory(), saved.getTargetUrl())
            );
        }
    }
}