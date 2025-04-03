package com.dolai.backend.stt_log.repository;

import com.dolai.backend.stt_log.model.STTLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface STTLogRepository extends JpaRepository<STTLog, Long> {
    List<STTLog> findByMeetingIdOrderByTimestampAsc(String meetingId);
}