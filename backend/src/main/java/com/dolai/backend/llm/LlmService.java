package com.dolai.backend.llm;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LlmService {

    // WebClient를 생성자 주입 받아 사용
    private final WebClient webClient;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    /**
     * 주어진 질문과 미팅 context를 기반으로 Gemini API에 질문을 보내고 응답을 받아옴
     *
     * @param question 사용자가 입력한 질문
     * @param contextList 미팅 발화 context 리스트
     * @return Gemini 응답 텍스트 (Mono 비동기)
     */
    public Mono<String> ask(String question, List<String> contextList) {
        // context 리스트를 하나의 문자열로 합치기
        String context = String.join("\n", contextList);

        // Gemini API 요청 형식에 맞게 request body 구성
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", "Context:\n" + context + "\n\nQuestion:\n" + question)
                        ))
                )
        );

        // Gemini API 호출
        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path(geminiApiUrl)
                        .queryParam("key", geminiApiKey)
                        .build())
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .map(this::extractTextFromResponse);
    }

    /**
     * Gemini API 응답에서 실제 답변 텍스트를 추출
     *
     * @param response Gemini API의 JSON 응답
     * @return 추출된 텍스트 답변
     */
    private String extractTextFromResponse(Map<String, Object> response) {
        var candidates = (List<Map<String, Object>>) response.get("candidates");
        if (candidates == null || candidates.isEmpty()) {
            throw new IllegalStateException("No candidates returned from Gemini API");
        }
        var content = (Map<String, Object>) candidates.get(0).get("content");
        var parts = (List<Map<String, Object>>) content.get("parts");
        if (parts == null || parts.isEmpty()) {
            throw new IllegalStateException("No parts in candidate content");
        }
        return (String) parts.get(0).get("text");
    }
}
