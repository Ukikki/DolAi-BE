package com.dolai.backend.stt_log.repository;

import com.dolai.backend.stt_log.model.STTLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

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

    // 목데이터와 연결된 미팅 아이디를 방 생성 시 미팅 아이디로 업뎃
    // 목데이터의 synced 0으로 변경
    // 목데이터의 meeting_id를 새로 생성된 미팅 ID로 연결
    @Modifying
    @Transactional(timeout = 30)
    @Query(value = "UPDATE stt_logs SET meeting_id = :meetingId, synced = 0 WHERE id BETWEEN 1 AND 24", nativeQuery = true)
    void assignDemoLogsToMeeting(@Param("meetingId") String meetingId);

    /**
     * ✅ 데모 로그 TODO 상태 초기화
     */
    @Modifying
    @Transactional(timeout = 15)
    @Query(value = "UPDATE stt_logs SET todo_checked = 0 WHERE id BETWEEN 1 AND 24", nativeQuery = true)
    void resetDemoTodos();

    /**
     * ✅ 비데모 로그 삭제 (id > 24)
     */
    @Modifying
    @Transactional(timeout = 30)
    @Query(value = "DELETE FROM stt_logs WHERE id > 24", nativeQuery = true)
    void deleteLogs();
}