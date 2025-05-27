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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
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
    @PostMapping("/meetings/{id}/invite")
    public ResponseEntity<?> inviteUserToMeeting(
            @PathVariable("id") String id,
            @RequestBody @Valid MeetingInviteRequestDto request,
            @AuthenticationPrincipal User hostUser
    ) {
        meetingService.inviteUserToMeeting(id, request.getTargetUserId(), hostUser);
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
    @PatchMapping("/{id}/end")
    public ResponseEntity<?> endMeeting(@PathVariable("id") String id, @AuthenticationPrincipal User user) {
        meetingService.endMeeting(id, user);
        return ResponseEntity.ok(Map.of("status", "success"));
    }

    //최근 사용자가 참여한 회의 3개 조회
    @GetMapping("/meetings/history-recent")
    public ResponseEntity<?> getRecentEndedMeetings(@AuthenticationPrincipal User user) {
        List<MeetingListResponseDto> response = meetingService.getRecentEndedMeetings(user);
        return ResponseEntity.ok(new SuccessDataResponse<>(response));
    }

    //최근 사용자가 참여한 회의 전체 조회
    @GetMapping("/meetings/history")
    public ResponseEntity<?> getAllEndedMeetings(@AuthenticationPrincipal User user) {
        List<MeetingListResponseDto> response = meetingService.getAllEndedMeetings(user);
        return ResponseEntity.ok(new SuccessDataResponse<>(response));
    }

    // 6. 회의 그래프 이미지 업로드
    @PostMapping("/meetings/{id}/graph-image")
    public ResponseEntity<?> uploadGraphImage(
            @PathVariable("id") String id,
            @RequestPart("image") MultipartFile imageFile
    ) {
        meetingService.saveGraphImageUrl(id, imageFile);
        return ResponseEntity.ok(Map.of("status", "success"));
    }
}