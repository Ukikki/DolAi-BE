package com.dolai.backend.meeting.controller;

import com.dolai.backend.meeting.model.*;
import com.dolai.backend.meeting.repository.MeetingRepository;
import com.dolai.backend.meeting.service.MeetingService;
import com.dolai.backend.user.model.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping
public class MeetingController {

    private final MeetingService meetingService;
    private final MeetingRepository meetingRepository;

    // 1. 새 화상회의 생성
    @PostMapping("/meetings")
    public ResponseEntity<?> createMeeting(
            @RequestBody @Valid MeetingCreateRequestDto request,
            @AuthenticationPrincipal User user) {

        MeetingResponseDto response = meetingService.createMeeting(request, user.getId());
        return ResponseEntity.ok(response);
    }

    // 2. 화상회의 참여
    @PostMapping("/join")
    public ResponseEntity<?> joinMeeting(
            @AuthenticationPrincipal User user,
            @RequestBody @Valid JoinMeetingRequestDto request) {
        MeetingResponseDto response = meetingService.joinMeeting(user, request);
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "meeting", response
        ));
    }

    // 3. 화상회의 종료
    @PatchMapping("/{meetingId}/end")
    public ResponseEntity<?> endMeeting(@PathVariable String meetingId, @AuthenticationPrincipal User user) {
        meetingService.endMeeting(meetingId, user);
        return ResponseEntity.ok(Map.of("status", "success"));
    }
}
