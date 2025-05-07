package com.dolai.backend.stt_log.service;

import com.dolai.backend.meeting.model.Meeting;
import com.dolai.backend.meeting.repository.MeetingRepository;
import com.dolai.backend.stt_log.model.STTLog;
import com.dolai.backend.stt_log.model.STTLogBroadcastDto;
import com.dolai.backend.stt_log.model.STTLogRequest;
import com.dolai.backend.stt_log.repository.STTLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class STTLogService {

    private final STTLogRepository sttLogRepository;
    private final MeetingRepository meetingRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public void saveLog(STTLogRequest request) {
        STTLog log = saveLogToDB(request);
        broadcastLog(request.getMeetingId(), log);
    }

    private STTLog saveLogToDB(STTLogRequest request) {
        Meeting meeting = meetingRepository.findById(request.getMeetingId())
                .orElseThrow(() -> new IllegalArgumentException("회의를 찾을 수 없습니다."));

        STTLog log = STTLog.builder()
                .meeting(meeting)
                .speakerName(request.getSpeaker())
                .text(request.getText())
                .textKo(request.getTextKo())
                .textEn(request.getTextEn())
                .textZh(request.getTextZh())
                .timestamp(request.getTimestamp() != null ? request.getTimestamp() : LocalDateTime.now())
                .build();

        return sttLogRepository.save(log);
    }

    private void broadcastLog(String meetingId, STTLog log) {
        STTLogBroadcastDto dto = STTLogBroadcastDto.builder()
                .speaker(log.getSpeakerName())
                .text(log.getText())
                .textKo(log.getTextKo())
                .textEn(log.getTextEn())
                .textZh(log.getTextZh())
                .timestamp(log.getTimestamp())
                .build();

        messagingTemplate.convertAndSend("/topic/stt/" + meetingId, dto);
    }


    public List<STTLog> getLogsByMeeting(String meetingId) {
        return sttLogRepository.findByMeetingIdOrderByTimestampAsc(meetingId);
    }

    public String getFullTranscriptForLLM(String meetingId) {
        List<STTLog> logs = sttLogRepository.findByMeetingIdOrderByTimestampAsc(meetingId);

        return logs.stream()
                .map(log -> log.getSpeakerName() + ": " + log.getTextKo())
                .collect(Collectors.joining("\n"));
    }
}
