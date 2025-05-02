package com.dolai.backend.config;

import com.arangodb.ArangoDB;
import com.arangodb.ArangoDatabase;
import com.arangodb.springframework.annotation.EnableArangoRepositories;
import com.arangodb.springframework.config.ArangoConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/* * ArangoDB Configuration: 기본 DB 연결 설정
 *
 * Spring Data ArangoDB에서 필수로 요구하는 설정
 * ArangoDB 서버의 호스트, 포트, 사용자 인증, 기본 DB 선택 등 지정
 */

@Configuration
@EnableArangoRepositories(basePackages = "com.dolai.backend.graph.repository")
public class ArangoConfig implements ArangoConfiguration {

    @Value("${arangodb.host}")
    private String host;

    @Value("${arangodb.port}")
    private Integer port;

    @Value("${arangodb.user}")
    private String user;

    @Value("${arangodb.password}")
    private String password;

    @Value("${arangodb.database}")
    private String database;

    @Override
    public ArangoDB.Builder arango() {
        return new ArangoDB.Builder()
                .host(host, port)
                .user(user)
                .password(password);
    }

    @Override
    public String database() {
        return database;
    }

    // ArangoDB Bean 등록
    @Bean
    public ArangoDB arangoDB() {
        return arango().build(); // 기존 arango() 메서드를 그대로 활용
    }

    // ArangoDatabase Bean 등록 (db 이름까지 붙은거)
    @Bean
    public ArangoDatabase arangoDatabase(ArangoDB arangoDB) {
        return arangoDB.db(database);
    }
}
