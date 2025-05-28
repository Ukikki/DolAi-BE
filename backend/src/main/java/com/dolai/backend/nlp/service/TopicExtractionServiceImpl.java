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
                회의 텍스트로부터 핵심 키워드를 단계적으로 정제해 추출하라.
                
                🔹 처리 단계:
                1단계: 전체 텍스트에서 **고유 명사 / 복합 명사 / 주요 기술 용어** 중심으로 명사만 식별하라.
                2단계: 아래에 해당하는 단어는 **모두 제거**하라.
                         - 일반 명사: 경우, 내용, 정도, 상황, 방식, 여부, 방법
                         - 대명사 및 지시어: 그것, 이거, 그거, 우리, 저희, 이, 그, 저
                         - 시간 표현: 다음, 이전, 이후, 지금, 오늘, 수요일, 오전, 오후, 이번, 이후, 전후
                         - 조사성/회화체: 네, 음, 아, 어, 좀, 자, 그리고, 만약, 또한
                         - 동사 명사화: 하다, 되다, 있음, 없음, 가능, 필요, 중요, 확인, 생각, 결정, 요청
                3단계: **실제 회의 논의 주제를 대표하거나 특정 기능, 작업 항목, 도구, 역할** 등을 나타내는 명사만 남겨라.
                4단계: **동일하거나 유사한 단어는 병합하여 하나로 정리**하고, 중요도에 따라 앞쪽에 배치하라.
                5단계: 최종 키워드를 **쉼표(,)로만 구분된 한 줄**로 출력하라. 다른 텍스트는 절대 출력하지 마.
                
                🔹 반드시 제외할 단어 유형:
                - **대명사 및 지시어**: 그것, 이거, 그거, 저희, 우리
                - **형식적인 명사/불명확한 추상어**: 경우, 상황, 내용, 것, 점, 때, 방식, 방법, 정도, 의미, 여부
                - **시간/순서/지시**: 이번, 다음, 이전, 이후, 전후, 위, 아래, 위해, 통해, 관련
                - **불용어**: 하다, 되다, 좋음, 있음, 없음, 가능, 필요, 중요
                - **회화체/감탄사/조사성**: 네, 음, 아, 어, 그, 자, 좀, 다만, 역시, 만약, 특히, 그리고
                
                🔹 좋은 키워드 예시:
                UI mockup, 사용자 플로우, 서버 IP, 버전 업데이트, QA 테스트, 기능 작동 확인, 일정 조율, 데이터 렌더링, 
                마케팅 전략, 고객 분석, 예산 계획, 프로젝트 일정, 품질 관리, 시장 조사, 사용자 피드백, 일정 조정
                
                🔹 나쁜 키워드 예시:
                우리, 이번, 다음, 하다, 목표, 결과, 방법, 내용, 정도, 위해, 만족, 저, 그거, 이거, 그것, 저희, 우리, 경우, 
                상황, 것, 점, 때, 의미, 여부, 좋음, 있음, 없음, 가능, 필요, 중요, 네, 음, 아, 어, 그, 자, 좀, 다만, 역시, 만약, 특히, 그리고
                
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