package com.dolai.backend.screenshare.controller;

import com.dolai.backend.screenshare.model.ScreenShareRequestDto;
import com.dolai.backend.screenshare.model.ScreenShareSocketEvent;
import com.dolai.backend.screenshare.model.enums.EventType;
import com.dolai.backend.screenshare.model.ScreenShare;
import com.dolai.backend.screenshare.service.ScreenShareService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/meetings")
public class ScreenShareController {

    private final ScreenShareService screenShareService;
    private final SimpMessagingTemplate messagingTemplate;

    @PostMapping("/{meetingId}/screen-share/start")
    public ResponseEntity<?> startScreenShare(@PathVariable String meetingId, @RequestBody ScreenShareRequestDto request) {
        ScreenShare session = screenShareService.start(meetingId, request.getUserId());
        messagingTemplate.convertAndSend("/topic/meeting/" + meetingId + "/screen-share",
                new ScreenShareSocketEvent(EventType.START, request.getUserId()));

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Screen sharing started successfully");
        response.put("screenSharingUser", request.getUserId());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{meetingId}/screen-share/stop")
    public ResponseEntity<?> stopScreenShare(@PathVariable String meetingId, @RequestBody ScreenShareRequestDto request) {
        ScreenShare session = screenShareService.stop(meetingId);
        messagingTemplate.convertAndSend("/topic/meeting/" + meetingId + "/screen-share",
                new ScreenShareSocketEvent(EventType.STOP, request.getUserId()));

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Screen sharing stopped successfully");
        response.put("screenSharingUser", request.getUserId());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{meetingId}/screen-share/status")
    public ResponseEntity<?> getScreenShareStatus(@PathVariable String meetingId) {
        Optional<ScreenShare> optional = screenShareService.get(meetingId);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("isSharing", optional.map(ScreenShare::isActive).orElse(false));
        response.put("screenSharingUser", optional.map(ScreenShare::getUserId).orElse(null));

        return ResponseEntity.ok(response);
    }
}