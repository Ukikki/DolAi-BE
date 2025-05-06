package com.dolai.backend.config;

import com.arangodb.ArangoDB;
import com.arangodb.ArangoDatabase;
import com.arangodb.Protocol;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/*
* * ArangoDB Configuration: 기본 DB 연결 설정
 */

@Configuration
public class ArangoConfig {

    @Value("${arangodb.host}")
    private String host;

    @Value("${arangodb.port}")
    private Integer port;

    @Value("${arangodb.user}")
    private String user;

    @Value("${arangodb.password}")
    private String password;

    @Getter
    @Value("${arangodb.database}")
    private String database;

    @Bean
    public ArangoDB arangoDB() {
        return new ArangoDB.Builder()
                .host(host, port)
                .user(user)
                .password(password)
                .useProtocol(Protocol.HTTP_JSON)
                .build();
    }

    @Bean
    public ArangoDatabase arangoDatabase(ArangoDB arangoDB) {
        return arangoDB.db(database);
    }

}