package com.dolai.backend.nlp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TopicExtractionServiceImpl implements TopicExtractionService {

    @Qualifier("geminiRestTemplate")
    private final RestTemplate restTemplate;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Override
    public List<String> extract(String text) {
        if (text == null || text.trim().length() < 5) {
            return List.of(); // Gemini 호출 X
        }
        // Gemini API 호출
        String prompt = """
                감정, 태도, 표현 방식 같은 건 제외하고, **구체적인 주제나 개념 중심**으로 뽑아줘.
                그것, 그걸, 더 같은 불명확한 명사나 관사도 제외해.
                키워드가 **없다고 판단되면**, 응답하지 말고 **아무것도 출력하지 마**.
                (문자열 'null', '응답 없음', 빈 리스트 등 어떤 것도 출력하지 마.)
                너의 의견이나 해석, 주석도 절대 추가하지 마.
                키워드는 **쉼표(,)로만 구분된 한 줄**로 반환해줘.
                형식 예시: 인공지능, 자연어 처리, 회의 분석
                텍스트:
                """ + text;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("x-goog-api-key", geminiApiKey);

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                )
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(geminiApiUrl, entity, Map.class);
            log.info("Gemini 응답: {}", response.getBody()); // 이걸 꼭 찍어봐
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map content = (Map) ((List) response.getBody().get("candidates")).get(0);
                Map parts = (Map) ((List) ((Map) content.get("content")).get("parts")).get(0);
                String resultText = (String) parts.get("text");
                return Arrays.stream(resultText.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList();
            } else {
                log.warn("Gemini API 응답 실패: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Gemini API 호출 중 오류 발생", e);
        }

        return Collections.emptyList();
    }
}