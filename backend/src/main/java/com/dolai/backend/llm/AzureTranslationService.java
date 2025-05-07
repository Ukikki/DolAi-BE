package com.dolai.backend.llm;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AzureTranslationService {

    @Value("${azure.translator.key}")
    private String apiKey;

    @Value("${azure.translator.region}")
    private String region;

    private final RestTemplate restTemplate = new RestTemplate();

    public String translate(String text, String toLang) {
        String endpoint = "https://api.cognitive.microsofttranslator.com/translate?api-version=3.0&to=" + toLang;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Ocp-Apim-Subscription-Key", apiKey);
        headers.set("Ocp-Apim-Subscription-Region", region);

        List<Map<String, String>> body = List.of(Map.of("text", text));
        HttpEntity<List<Map<String, String>>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<List> response = restTemplate.postForEntity(endpoint, entity, List.class);
        List translations = response.getBody();
        Map first = (Map) translations.get(0);
        Map firstTrans = (Map) ((List) first.get("translations")).get(0);
        return (String) firstTrans.get("text");
    }
}
