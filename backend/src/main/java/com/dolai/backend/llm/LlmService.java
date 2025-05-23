package com.dolai.backend.llm;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class LlmService {

    private final WebClient webClient;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    /**
     * 일반 질문 + context 기반 응답
     */
    public Mono<String> ask(String question, List<String> contextList) {
        String prompt = buildPrompt(contextList, question);
        return callGemini(prompt);
    }

    /**
     * 주어진 발화 텍스트에서 주제를 추출
     */
    public Mono<List<String>> extractTopics(String text) {
        String topicPrompt = """
                다음 대화 내용을 바탕으로 회의 주제를 콤마(,)로 구분된 키워드로 반환해줘. 예시: 업무 진행, 고객 대응, 회의 일정
                대화:
                """ + text;

        return callGemini(topicPrompt)
                .map(this::splitCommaSeparated);
    }

    /**
     * Gemini 호출용 공통 메서드
     */
    private Mono<String> callGemini(String prompt) {
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                )
        );

        String fullUrl = geminiApiUrl + "?key=" + geminiApiKey;

        return webClient.post()
                .uri(fullUrl)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .map(this::extractTextFromResponse);
    }

    /**
     * Gemini 응답에서 텍스트만 추출
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

    /**
     * context와 질문을 하나의 프롬프트로 결합
     */
    private String buildPrompt(List<String> contextList, String question) {
    	String context = String.join("\n", contextList);
    	return """
                당신은 화상 회의 분석 어시스턴트 **"돌아이"**입니다.
                사용자가 전달한 ArangoDB 기반 회의 데이터만 바탕으로 질문에 답변하세요.
                MySQL, 외부 정보, 또는 추정/허구/상상은 절대 포함하지 마세요.
                모든 응답은 반드시 한국어로, 최대 세 문장 이내로 간결하게 작성하세요.
                불필요한 인사말 없이, 핵심 정보만 요약해 전달하세요.

            [회의 정보]
            """ + context + """

            [질문]
            """ + question;
    }


    /**
     * "토픽1, 토픽2, ..." → List<String> 변환
     */
    private List<String> splitCommaSeparated(String response) {
        return Stream.of(response.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    public String cleanJsonResponse(String response) {
        if (response == null) return null;
        return response.replaceAll("(?i)```json|```", "").trim();
    }
}

