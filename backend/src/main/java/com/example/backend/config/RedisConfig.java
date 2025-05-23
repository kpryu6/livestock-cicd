package com.example.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.RedisNode;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.core.HashOperations;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class RedisConfig {

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        // Sentinel 환경변수 읽기
        String sentinelMaster = System.getenv("spring.redis.sentinel.master");
        String sentinelNodes = System.getenv("spring.redis.sentinel.nodes");

        if (sentinelMaster == null || sentinelNodes == null) {
            log.error("❌ [ENV ERROR] Sentinel 환경변수가 설정되지 않았습니다!");
            throw new IllegalStateException("Sentinel 환경변수를 찾을 수 없습니다.");
        }

        log.info("✅ [ENV CHECK] Sentinel Master: {}", sentinelMaster);
        log.info("✅ [ENV CHECK] Sentinel Nodes: {}", sentinelNodes);

        // Redis Sentinel Configuration 생성
        RedisSentinelConfiguration sentinelConfig = new RedisSentinelConfiguration();
        sentinelConfig.master(sentinelMaster);

        // 여러 노드가 있을 경우 쉼표로 구분되었다고 가정 (단일 노드인 경우도 동작)
        String[] nodes = sentinelNodes.split(",");
        for (String node : nodes) {
            String[] parts = node.split(":");
            if (parts.length == 2) {
                String host = parts[0].trim();
                int port = Integer.parseInt(parts[1].trim());
                sentinelConfig.addSentinel(new RedisNode(host, port));
            } else {
                log.warn("❌ [WARNING] Sentinel 노드 형식이 잘못되었습니다: {}", node);
            }
        }

        return new LettuceConnectionFactory(sentinelConfig);
    }

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 키와 값 직렬화: String 사용
        StringRedisSerializer serializer = new StringRedisSerializer();
        template.setKeySerializer(serializer);
        template.setHashKeySerializer(serializer);
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);

        return template;
    }

    @Bean
    public HashOperations<String, String, String> hashOperations(RedisTemplate<String, String> redisTemplate) {
        return redisTemplate.opsForHash();
    }
}
