package com.dolai.backend.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Bean
    public Dotenv dotenv() {
        return Dotenv.load();  // .env 파일을 로드
    }
}
