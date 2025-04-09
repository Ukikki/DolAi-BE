package com.dolai.backend.config;

import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient() {
        X509TrustManager trustAllCerts = new X509TrustManager() {
            @Override public void checkClientTrusted(X509Certificate[] xcs, String string) {}
            @Override public void checkServerTrusted(X509Certificate[] xcs, String string) {}
            @Override public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
        };

        // Self-signed SSL 인증서 신뢰 설정
        HttpClient httpClient = HttpClient.create().secure(sslContextSpec -> {
            try {
                sslContextSpec.sslContext(
                        SslContextBuilder.forClient()
                                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                                .build()
                );
            } catch (Exception e) {
                throw new RuntimeException("SSL 설정 실패", e);
            }
        }
        );

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl("https://223.194.158.104:3000") // ✅ 실제 Mediasoup 서버 IP로 맞게 설정!
                .build();
    }
}
