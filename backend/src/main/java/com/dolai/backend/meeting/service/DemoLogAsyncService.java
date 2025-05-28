package com.dolai.backend.meeting.service;

import com.dolai.backend.stt_log.repository.STTLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DemoLogAsyncService {

    private final STTLogRepository sttLogRepository;

    @Async
    public void assignDemoLogsAsync(String meetingId) {
        try {
            sttLogRepository.assignDemoLogsToMeeting(meetingId);
        } catch (Exception e) {
            log.error("🧨 데모 로그 할당 중 오류", e);
        }
    }

    @Async
    public void clearDemoLogsAsync() {
        try {
            sttLogRepository.deleteLogs();
            sttLogRepository.resetDemoTodos();
            log.info("🧹 데모 STT 로그 정리 완료 (id > 24 삭제 + synced, todo 체크 초기화)");
        } catch (Exception e) {
            log.error("🧨 데모 로그 정리 중 오류 발생", e);
        }
    }
}
