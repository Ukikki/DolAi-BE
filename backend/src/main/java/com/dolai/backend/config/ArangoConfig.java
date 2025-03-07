package com.dolai.backend.config;

import com.arangodb.ArangoDB;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ArangoConfig {
    @Bean
    public ArangoDB arangoDB() {
        // .env 파일 로드
        Dotenv dotenv = Dotenv.load();

        String host = dotenv.get("ARANGO_HOST");
        int port = Integer.parseInt(dotenv.get("ARANGO_PORT"));
        String user = dotenv.get("ARANGO_USER");
        String password = dotenv.get("ARANGO_PASSWORD");

        return new ArangoDB.Builder()
                .host(host, port)
                .user(user)
                .password(password)
                .build();
    }
}
