package com.dolai.backend.meeting.controller;

import com.dolai.backend.common.success.SuccessDataResponse;
import com.dolai.backend.common.success.SuccessMessageResponse;
import com.dolai.backend.meeting.model.*;
import com.dolai.backend.meeting.model.enums.Status;
import com.dolai.backend.meeting.service.MeetingService;
import com.dolai.backend.user.model.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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

        MeetingResponseDto response = meetingService.createMeeting(request, user.getId(), Status.ONGOING);
        return ResponseEntity.ok(response);
    }

    // 2. 회의 예약 등록
    @PostMapping("/meetings/schedule")
    public ResponseEntity<?> scheduleMeeting(
            @RequestBody @Valid MeetingCreateRequestDto request,
            @AuthenticationPrincipal User user) {

        MeetingResponseDto response = meetingService.createMeeting(request, user.getId(), Status.SCHEDULED);
        return ResponseEntity.ok(response);
    }

    // 3. 화상회의 초대
    @PostMapping("/meetings/{meetingId}/invite")
    public ResponseEntity<?> inviteUserToMeeting(
            @PathVariable("meetingId") String meetingId,
            @RequestBody @Valid MeetingInviteRequestDto request,
            @AuthenticationPrincipal User hostUser
    ) {
        meetingService.inviteUserToMeeting(meetingId, request.getTargetUserId(), hostUser);
        return ResponseEntity.ok(new SuccessMessageResponse("초대 전송 완료"));
    }


    // 4. 화상회의 참여
    @PostMapping("/join")
    public ResponseEntity<?> joinMeeting(
            @AuthenticationPrincipal User user,
            @RequestBody @Valid JoinMeetingRequestDto request) {
        MeetingResponseDto response = meetingService.joinMeeting(user, request);
        return ResponseEntity.ok(new SuccessDataResponse<>(response));
    }

    // 5. 화상회의 종료
    @PatchMapping("/{meetingId}/end")
    public ResponseEntity<?> endMeeting(@PathVariable("meetingId") String meetingId, @AuthenticationPrincipal User user) {
        meetingService.endMeeting(meetingId, user);
        return ResponseEntity.ok(Map.of("status", "success"));
    }
}
