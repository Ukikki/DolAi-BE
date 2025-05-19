package com.dolai.backend.screenshare.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class AzureOcrService {

    @Value("${azure.ocr.key1}")
    private String apiKey;

    @Value("${azure.ocr.endpoint}")
    private String endpoint;

    private final WebClient webClient = WebClient.create();

    public Mono<String> extractTextFromImage(byte[] imageBytes) {
        String url = endpoint + "/vision/v3.2/ocr?language=ko&detectOrientation=true";

        return webClient.post()
                .uri(url)
                .header("Ocp-Apim-Subscription-Key", apiKey)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .bodyValue(imageBytes)
                .retrieve()
                .bodyToMono(String.class);
    }
}