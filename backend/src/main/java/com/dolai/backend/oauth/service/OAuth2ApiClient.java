package com.dolai.backend.oauth.service;

import com.dolai.backend.common.exception.CustomException;
import com.dolai.backend.common.exception.ErrorCode;
import com.dolai.backend.oauth.model.OAuth2Properties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2ApiClient {

    private final WebClient.Builder webClientBuilder;
    private final OAuth2Properties oauth2Properties;
    private WebClient webClient;
    @PostConstruct
    public void init() {
        this.webClient = webClientBuilder.build();
    }

    // Kakao 요청 시 Form Data 사용
    public Map<String, Object> getAccessToken(String provider, String authorizationCode) {
        OAuth2Properties.Provider providerConfig = getProviderConfig(provider);

        WebClient.RequestBodySpec request = webClient.post()
                .uri(providerConfig.getTokenUri());

        if ("kakao".equalsIgnoreCase(provider) || "google".equalsIgnoreCase(provider)) {
            request.headers(headers -> headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED));
        }

        // [로그] 요청 데이터 확인
        log.info("🔹 [OAuth2 요청] Provider: {}, Token URI: {}", provider, providerConfig.getTokenUri());
        log.info("🔹 [OAuth2 요청 데이터] client_id: {}, redirect_uri: {}, code: {}",
                providerConfig.getClientId(), providerConfig.getRedirectUri(), authorizationCode);
        log.info("✅ 최종 redirect_uri 확인: {}", providerConfig.getRedirectUri());

        // Form Data 방식으로 요청
        return request
                .body(BodyInserters.fromFormData("client_id", providerConfig.getClientId())
                        .with("client_secret", providerConfig.getClientSecret())
                        .with("code", authorizationCode)
                        .with("grant_type", "authorization_code")
                        .with("redirect_uri", providerConfig.getRedirectUri()))
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(String.class).flatMap(errorBody -> {
                            log.error("❌ OAuth2 토큰 요청 실패 응답 바디: {}", errorBody);
                            return Mono.error(WebClientResponseException.create(
                                    response.statusCode().value(),
                                    "OAuth2 Error",
                                    response.headers().asHttpHeaders(),
                                    errorBody.getBytes(),
                                    null,
                                    null
                            ));
                        })
                )
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})  // 제네릭 타입 명시
                .block();
    }

    // 사용자 정보 요청 (Access Token 사용)
    public Map<String, Object> getUserInfo(String provider, String accessToken) {
        OAuth2Properties.Provider providerConfig = getProviderConfig(provider);

        log.info("Requesting user info from: {}", providerConfig.getUserInfoUri());
        return webClient.get()
                .uri(providerConfig.getUserInfoUri())
                .headers(headers -> headers.setBearerAuth(accessToken)) // Bearer Token 사용
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();
    }

    // 지원하는 OAuth2 Provider 가져오기
    private OAuth2Properties.Provider getProviderConfig(String provider) {
        OAuth2Properties.Provider providerConfig = oauth2Properties.getProvider().get(provider);

        if (providerConfig == null) {
            throw new CustomException(ErrorCode.OAUTH_PROVIDER_NOT_FOUND);
        }
        return providerConfig;
    }
}
