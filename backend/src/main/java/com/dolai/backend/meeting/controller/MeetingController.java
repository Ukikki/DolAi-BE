package com.dolai.backend.meeting.controller;

import com.dolai.backend.meeting.model.MeetingCreateRequestDto;
import com.dolai.backend.meeting.model.MeetingResponseDto;
import com.dolai.backend.meeting.service.MeetingService;
import com.dolai.backend.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/meetings")
public class MeetingController {

    private final MeetingService meetingService;

    @PostMapping
    public ResponseEntity<?> createMeeting(
            @RequestBody MeetingCreateRequestDto request,
            @AuthenticationPrincipal User user
    ) {
        try {
            String userId = user.getId(); // JWT에서 파싱된 사용자 ID
            MeetingResponseDto response = meetingService.createMeeting(request, userId);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "meeting", response
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", "error",
                    "message", "Internal server error"
            ));
        }
    }
}
