package com.dolai.backend.stt_log.service;

import com.dolai.backend.common.exception.CustomException;
import com.dolai.backend.common.exception.ErrorCode;
import com.dolai.backend.meeting.model.Meeting;
import com.dolai.backend.meeting.repository.MeetingRepository;
import com.dolai.backend.s3.S3Service;
import com.dolai.backend.stt_log.model.STTLog;
import com.dolai.backend.stt_log.model.STTLogBroadcastDto;
import com.dolai.backend.stt_log.model.STTLogRequest;
import com.dolai.backend.stt_log.repository.STTLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class STTLogService {

    private final STTLogRepository sttLogRepository;
    private final MeetingRepository meetingRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final S3Service s3Service;

    public void saveLog(STTLogRequest request) {
        STTLog savedLog = saveLogToDB(request);

        log.info("STTLog saved: {}", savedLog); // log

        broadcastLog(request.getMeetingId(), savedLog);
    }

    private STTLog saveLogToDB(STTLogRequest request) {
        Meeting meeting = meetingRepository.findById(request.getMeetingId())
                .orElseThrow(() -> new CustomException(ErrorCode.MEETING_NOT_FOUND));

        log.info("Meeting found: " + meeting); // log

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

    // txt 파일 생성
    public Map<String, String> generateTxtFilesAndUpload(String meetingId, Map<String, String> titleMap) {
        List<STTLog> logs = sttLogRepository.findByMeetingIdOrderByTimestampAsc(meetingId);
        if (logs.isEmpty()) throw new CustomException(ErrorCode.TODO_NOT_FOUND);

        Map<String, StringBuilder> builders = Map.of(
                "ko", new StringBuilder(), "en", new StringBuilder(), "zh", new StringBuilder()
        );

        for (STTLog log : logs) {
            String time = log.getTimestamp().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"));
            builders.get("ko").append(time).append(" ").append(log.getSpeakerName()).append(": ").append(log.getTextKo()).append("\n");
            builders.get("en").append(time).append(" ").append(log.getSpeakerName()).append(": ").append(log.getTextEn()).append("\n");
            builders.get("zh").append(time).append(" ").append(log.getSpeakerName()).append(": ").append(log.getTextZh()).append("\n");
        }

        Map<String, String> result = new HashMap<>();

        for (String lang : List.of("ko", "en", "zh")) {
            try {
                String baseTitle = titleMap.get(lang).replaceAll("[\\\\/:*?\"<>|\\s]", "_");
                String label = switch (lang) {
                    case "ko" -> "전체자막";
                    case "en" -> "FullTranscript";
                    case "zh" -> "字幕全文记录";
                    default -> "Transcript";
                };
                String filename = baseTitle + "_" + label + "_" + lang + ".txt";

                File txtFile = new File("output", filename);
                txtFile.getParentFile().mkdirs();
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(txtFile))) {
                    writer.write(builders.get(lang).toString());
                }

                String s3Url = s3Service.uploadMeetingDocument(txtFile, meetingId, filename);
                result.put(lang, s3Url);
            } catch (Exception e) {
                log.error("❌ STT .txt 파일 생성/업로드 실패: {}", lang, e);
                throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
            }
        }

        return result;
    }
}
