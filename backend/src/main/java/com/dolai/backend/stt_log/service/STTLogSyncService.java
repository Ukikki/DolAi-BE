package com.dolai.backend.stt_log.service;

import com.dolai.backend.graph.service.GraphService;
import com.dolai.backend.stt_log.model.STTLog;
import com.dolai.backend.stt_log.repository.STTLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class STTLogSyncService {

    private final STTLogRepository sttLogRepository;
    private final GraphService graphService;

    @Scheduled(fixedDelay = 3000)
    public void syncToArango() {
        List<STTLog> logsToSync = sttLogRepository.findTop10BySyncedFalseOrderByTimestampAsc();

        for (STTLog sttLog : logsToSync) {
            try {
                log.info("💬 Syncing: [{}] {} - {}", sttLog.getMeeting().getId(), sttLog.getSpeakerName(), sttLog.getText());
                graphService.saveUtterance(
                        sttLog.getMeeting().getId().toString(),
                        sttLog.getSpeakerName(),
                        sttLog.getText()
                );

                sttLog.setSynced(true);
                sttLogRepository.save(sttLog);
            } catch (Exception e) {
                log.error("❌ Failed to sync STTLog", e);
            }
        }

    }
}