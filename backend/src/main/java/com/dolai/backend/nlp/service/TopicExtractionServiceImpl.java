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
                회의용 핵심 키워드만 단계별로 추출해.
                
                🔹 처리 단계:
                1단계: 텍스트에서 **모든 명사**를 식별해.
                2단계: **불필요한 일반 단어, 불명확한 명사, 추상적 개념, 감탄사, 조사성 단어**를 제거해.
                3단계: **구체적이고 명확한 개념이나 사안**만 남겨.
                4단계: 중복은 제거하고, **회의에서 중요하게 다룰 만한 순서로 정렬해.**
                5단계: **쉼표(,)로만 구분된 한 줄**로 출력해. 다른 텍스트는 출력하지 마.
                
                🔹 반드시 제외할 단어 유형:
                - **대명사 및 지시어**: 그것, 이거, 그거, 저희, 우리
                - **형식적인 명사/불명확한 추상어**: 경우, 상황, 내용, 것, 점, 때, 방식, 방법, 정도, 의미, 여부
                - **시간/순서/지시**: 이번, 다음, 이전, 이후, 전후, 위, 아래, 위해, 통해, 관련
                - **불용어**: 하다, 되다, 좋음, 있음, 없음, 가능, 필요, 중요
                - **회화체/감탄사/조사성**: 네, 음, 아, 어, 그, 자, 좀, 다만, 역시, 만약, 특히, 그리고
                
                🔹 좋은 키워드 예시:
                마케팅 전략, 고객 분석, 예산 계획, 프로젝트 일정, 품질 관리, 시장 조사, 사용자 피드백, 일정 조정
                
                🔹 나쁜 키워드 예시:
                우리, 이번, 다음, 그것, 하다, 목표, 결과, 방법, 내용, 정도, 위해, 만족
                
                ❗ 주의:
                키워드가 없다고 판단되면, 아무것도 출력하지 마. ("null", "없음", 빈 리스트 등 어떤 형태도 출력하지 마)
                
                텍스트:
                [여기에 텍스트 입력]
                
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