package com.dolai.backend.chatbot.controller;

import com.dolai.backend.chatbot.model.ChatWithContextRequest;
import com.dolai.backend.chatbot.service.ChatbotService;
import com.dolai.backend.common.success.SuccessDataResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatbotController {
    private final ChatbotService chatbotService;

    @GetMapping("/chat")
    public ResponseEntity<?> gemini() {
        try {
            return ResponseEntity.ok().body(chatbotService.getContents("안녕! 너는 누구야?"));
        } catch (HttpClientErrorException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/ask")
    public ResponseEntity<?> ask(@RequestBody Map<String, String> body) {
        try {
            String prompt = body.get("message");
            String result = chatbotService.getContents(prompt);
            return ResponseEntity.ok().body(Map.of("response", result));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/context")
    public ResponseEntity<?> chatWithContext(@RequestBody ChatWithContextRequest request) {
        try {
            String result = chatbotService.getSmartResponse(request.getMeetingId(), request.getMessage());
            return ResponseEntity.ok(new SuccessDataResponse(result));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}