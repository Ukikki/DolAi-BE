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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
        try {
            MeetingResponseDto response = meetingService.joinMeeting(user, request);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "meeting", response
            ));

        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(Map.of("status", "error", "message", e.getReason()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", "Internal server error"));
        }
    }
}
