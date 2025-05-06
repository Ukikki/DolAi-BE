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
@RequestMapping("/whiteboard")
@RequiredArgsConstructor
public class WhiteboardController {
    private final WhiteboardService whiteboardService;

    @PostMapping("/start/{meetingId}")
    public ResponseEntity<WhiteboardSession> start(@PathVariable String meetingId) {
        return ResponseEntity.ok(whiteboardService.startSession(meetingId));
    }
}
