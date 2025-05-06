package com.dolai.backend.stt_log.repository;

import com.dolai.backend.stt_log.model.STTLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface STTLogRepository extends JpaRepository<STTLog, Long> {
    List<STTLog> findByMeetingIdOrderByTimestampAsc(String meetingId);

    List<STTLog> findTop10BySyncedFalseOrderByTimestampAsc();

    List<STTLog> findByMeetingId(String meetingId);

    // Meeting ID로 찾고, synced=false인 것만 가져오는 메서드
    List<STTLog> findByMeetingIdAndSyncedFalse(String meetingId);
}