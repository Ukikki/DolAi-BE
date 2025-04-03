package com.dolai.backend.meeting.controller;

import com.dolai.backend.meeting.model.*;
import com.dolai.backend.meeting.service.MeetingService;
import com.dolai.backend.user.model.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping
public class MeetingController {

    private final MeetingService meetingService;

    // 1. 새 화상회의 생성
    @PostMapping("/meetings")
    public ResponseEntity<?> createMeeting(
            @RequestBody @Valid MeetingCreateRequestDto request,
            @AuthenticationPrincipal User user) {

        // 1. 화상회의 생성(DB에 저장)
        MeetingResponseDto response = meetingService.createMeeting(request, user.getId());
        return ResponseEntity.ok(response);
    }

    // 2. 화상회의 참여
    /*@PostMapping("/join")
    public ResponseEntity<?> joinMeeting(
            @AuthenticationPrincipal User user,
            @RequestBody @Valid JoinRequestDto requestDto) {

        JoinResponseDto response = meetingService.joinMeeting(user, requestDto);

        // Mediasoup-SFU에 WebSocket으로 Transport 생성 요청
        mediasoupClient.createTransport(response.getMeetingId(), response.getUserId());

        return ResponseEntity.ok("success");
    }*/
}
