package com.dolai.backend.meeting.repository;

import com.dolai.backend.meeting.model.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MeetingRepository extends JpaRepository<Meeting, String> {
    Optional<Meeting> findByInviteUrl(String inviteUrl);
}
