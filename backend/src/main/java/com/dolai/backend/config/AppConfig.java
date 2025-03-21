package com.dolai.backend.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    /*@Bean
    public Dotenv dotenv() {
        return Dotenv.load();  // .env 파일을 로드
    }*/

    @Bean
    public Dotenv dotenv() {
        return Dotenv.configure()
                .directory("backend")  // backend 폴더 내에서 찾음
                .ignoreIfMissing()  // 파일 없어도 무시(개발 초기 단계에서만)
                .load();
    }

}
