package com.dolai.backend.whiteboard.controller;

import com.dolai.backend.whiteboard.model.WhiteboardSession;
import com.dolai.backend.whiteboard.service.WhiteboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/whiteboard")
@RequiredArgsConstructor
public class WhiteboardController {
    private final WhiteboardService whiteboardService;

    // 화이트보드 시작
    @PostMapping("/start/{meetingId}")
    public ResponseEntity<WhiteboardSession> start(@PathVariable String meetingId) {
        return ResponseEntity.ok(whiteboardService.startSession(meetingId));
    }

    // 화이트보드 종료
    @PostMapping("/end/{meetingId}")
    public ResponseEntity<WhiteboardSession> end(@PathVariable String meetingId) {
        return ResponseEntity.ok(whiteboardService.endSession(meetingId));
    }
}
