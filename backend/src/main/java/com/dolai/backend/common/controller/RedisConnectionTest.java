package com.dolai.backend.common.controller;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisConnectionTest {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @PostConstruct
    public void testConnection() {
        try {
            // Redis에 데이터 저장
            redisTemplate.opsForValue().set("testKey", "Hello, Redis!");

            // 저장한 데이터 읽기
            String value = redisTemplate.opsForValue().get("testKey");
            System.out.println("✅ Redis 연결 성공: " + value);

        } catch (Exception e) {
            System.err.println("❌ Redis 연결 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }
}