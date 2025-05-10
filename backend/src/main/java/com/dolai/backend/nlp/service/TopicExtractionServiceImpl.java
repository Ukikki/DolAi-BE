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
        String prompt = """
                아래 텍스트를 참고해서 관련된 주제를 3~5개 단어 수준으로 뽑아줘. 마크다운이나 설명은 필요 없어. 콤마로 구분해서 반환해줘.
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