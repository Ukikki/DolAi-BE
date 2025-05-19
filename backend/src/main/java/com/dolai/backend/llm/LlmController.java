package com.dolai.backend.llm;

/*
    /llm/ask REST API 받아서 처리
 */

import com.dolai.backend.common.success.SuccessDataResponse;
import com.dolai.backend.common.success.SuccessMessageResponse;
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
    private final LlmService llmService;
    private final GraphService graphService;
    private final LlmTodoService llmTodoService;


    // 미팅 발화 context를 가져와서 LLM에 질문을 던짐
    @PostMapping("/ask")
    public Mono<String> ask(@RequestBody AskRequest request) {
        return graphService.getContextByMeetingId(request.getMeetingId())
                .flatMap(contextList -> llmService.ask(request.getQuestion(), contextList));
    }

    // 주어진 발화 텍스트에서 주제를 추출
    @PostMapping("/extract-topics")
    public Mono<?> extractTopics(@RequestBody TopicRequest request) {
        return llmService.extractTopics(request.getText());
    }

    @Data
    static class AskRequest {
        private String meetingId;
        private String question;
    }

    @Data
    static class TopicRequest {
        private String text;
    }

    @PostMapping("/todo/extract/{meetingId}")
    public ResponseEntity<?> extractTodos(@PathVariable String meetingId) {
        llmTodoService.extractAndSaveTodos(meetingId);
        return ResponseEntity.ok(new SuccessMessageResponse("할 일 추출 시도 완료"));
    }
}