package com.dolai.backend.config;

import io.netty.handler.ssl.SslContextBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLException;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

@Configuration
public class WebClientConfig {

 /*   @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }*/

    @Bean
    public WebClient webClient() throws Exception {
        X509TrustManager trustAllCerts = new X509TrustManager() {
            @Override public void checkClientTrusted(X509Certificate[] xcs, String string) {}
            @Override public void checkServerTrusted(X509Certificate[] xcs, String string) {}
            @Override public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
        };

        HttpClient httpClient = HttpClient.create().secure(sslContextSpec ->
                {
                    try {
                        sslContextSpec.sslContext(SslContextBuilder.forClient().trustManager(trustAllCerts).build());
                    } catch (SSLException e) {
                        throw new RuntimeException(e);
                    }
                }
        );

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl("https://223.194.137.95:3000") // Mediasoup 기본 주소 지정
                .build();
    }
}
