package com.dolai.backend.llm;

/*
    /api/llm/ask REST API 받아서 처리
 */

import com.dolai.backend.common.success.SuccessDataResponse;
import com.dolai.backend.graph.service.GraphService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import java.util.Map;

@RestController
@RequestMapping("/llm")
@RequiredArgsConstructor
public class LlmController {

    private final LlmDocumentService llmDocumentService;
    private final GraphService graphService;

    // 미팅 발화 context를 가져와서 LLM에 질문을 던짐
    @PostMapping("/ask")
    public Mono<String> ask(@RequestBody AskRequest request) {
        return graphService.getContextByMeetingId(request.getMeetingId())
                .flatMap(contextList -> llmService.ask(request.getQuestion(), contextList));
    }

    @Data
    static class AskRequest {
        private String meetingId;
        private String question;
    }

    @PostMapping("/summarize")
    public ResponseEntity<?> summarize(@RequestBody Map<String, String> body) {
            String meetingId = body.get("meetingId");

            String resultPath = llmDocumentService.summarizeAndGenerateDoc(meetingId);

            return ResponseEntity.ok(new SuccessDataResponse<>(resultPath));

    }
}