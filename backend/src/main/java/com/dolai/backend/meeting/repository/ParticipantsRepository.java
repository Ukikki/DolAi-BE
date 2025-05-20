package com.dolai.backend.meeting.repository;

import com.dolai.backend.meeting.model.Participant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface ParticipantsRepository extends JpaRepository<Participant, String> {
    boolean existsByMeetingIdAndUserId(String meetingId, String userId);
    void deleteAllByMeetingId(String meetingId);
    List<Participant> findByMeetingId(String meetingId);
}