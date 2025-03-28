package com.dolai.backend.meeting.repository;

import com.dolai.backend.meeting.model.Participant;
import org.springframework.data.jpa.repository.JpaRepository;


public interface ParticipantsRepository extends JpaRepository<Participant, String> {
    boolean existsByMeetingIdAndUserId(String meetingId, String userId);
}
