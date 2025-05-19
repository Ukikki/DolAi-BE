package com.dolai.backend.llm;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AzureTranslationService {

    @Value("${azure.translator.key}")
    private String apiKey;

    @Value("${azure.translator.region}")
    private String region;

    private final RestTemplate restTemplate = new RestTemplate();

    public String translate(String text, String toLang) {
        try {
            String endpoint = "https://api.cognitive.microsofttranslator.com/translate?api-version=3.0&to=" + toLang;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Ocp-Apim-Subscription-Key", apiKey);
            headers.set("Ocp-Apim-Subscription-Region", region);

            List<Map<String, String>> body = List.of(Map.of("text", text));  // 원본 텍스트로 번역 요청
            HttpEntity<List<Map<String, String>>> entity = new HttpEntity<>(body, headers);

            // 번역 요청
            ResponseEntity<List> response = restTemplate.postForEntity(endpoint, entity, List.class);
            List translations = response.getBody();
            Map first = (Map) translations.get(0);
            Map firstTrans = (Map) ((List) first.get("translations")).get(0);
            String translatedText = (String) firstTrans.get("text");

            // 번역 후 정리: <w:br/>로 처리된 부분의 띄어쓰기 제거
            String cleanedText = translatedText.replaceAll("<.*?/?>", "<w:br/>");

            return cleanedText;
        } catch (Exception e) {
            log.error("번역 중 오류 발생: {}", e.getMessage());
        }
        return text;  // 실패 시 원본 텍스트를 반환
    }


}
