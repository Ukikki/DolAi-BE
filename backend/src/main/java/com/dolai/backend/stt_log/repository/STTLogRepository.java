package com.dolai.backend.stt_log.repository;

import com.dolai.backend.stt_log.model.STTLog;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface STTLogRepository extends JpaRepository<STTLog, Long> {
    List<STTLog> findByMeetingIdOrderByTimestampAsc(String meetingId);

    List<STTLog> findTop10BySyncedFalseOrderByTimestampAsc();

    List<STTLog> findByMeetingId(String meetingId);

    // Meeting ID로 찾고, synced=false인 것만 가져오는 메서드
    List<STTLog> findByMeetingIdAndSyncedFalse(String meetingId);

    @Query("SELECT s FROM STTLog s WHERE s.meeting.id = :meetingId AND s.todoChecked = false ORDER BY s.timestamp ASC")
    List<STTLog> findUncheckedLogsByMeetingId(@Param("meetingId") String meetingId);

}