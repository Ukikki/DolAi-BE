package com.dolai.backend.llm;

/*
    /api/llm/ask REST API 받아서 처리
 */

import com.dolai.backend.graph.service.GraphService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/llm")
@RequiredArgsConstructor
public class LlmController {

    private final LlmService llmService;
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
}