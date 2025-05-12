package com.dolai.backend.stt_log.controller;

import com.dolai.backend.common.success.SuccessDataResponse;
import com.dolai.backend.common.success.SuccessMessageResponse;
import com.dolai.backend.stt_log.model.STTLog;
import com.dolai.backend.stt_log.model.STTLogRequest;
import com.dolai.backend.stt_log.service.STTLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/stt")
@RequiredArgsConstructor
public class STTLogController {
    private final STTLogService sttLogService;

    //MySQL에 STT 로그 저장
    @PostMapping("/log")
    public ResponseEntity<?> saveLog(@RequestBody STTLogRequest request) {
        sttLogService.saveLog(request);

        return ResponseEntity.ok(new SuccessMessageResponse("save STT Log."));
    }

    @GetMapping
    public ResponseEntity<?> getLogsByMeeting(@RequestParam("meetingId") String meetingId) {
        List<STTLog> logs = sttLogService.getLogsByMeeting(meetingId);
        return ResponseEntity.ok().body(new SuccessDataResponse<>(logs));
    }
}