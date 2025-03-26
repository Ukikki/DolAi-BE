package com.dolai.backend.meeting.service;

import com.dolai.backend.meeting.model.Meeting;
import com.dolai.backend.meeting.model.MeetingCreateRequestDto;
import com.dolai.backend.meeting.model.MeetingResponseDto;
import com.dolai.backend.meeting.repository.MeetingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MeetingService {

    private final MeetingRepository meetingRepository;

    public MeetingResponseDto createMeeting(MeetingCreateRequestDto request, String userId) {
        if (request.getTitle() == null || request.getTitle().isBlank() || request.getStartTime() == null) {
            throw new IllegalArgumentException("title or startTime is missing");
        }

        Meeting meeting = Meeting.create(request.getTitle(), request.getStartTime(), userId);
        meetingRepository.save(meeting);

        String inviteUrl = "https://example.com/meetings/" + meeting.getId();
        return new MeetingResponseDto(meeting.getId(), meeting.getTitle(), meeting.getStartTime(), inviteUrl);
    }
}
