package com.example.stock.repository;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RedisRepository {

    private final RedisTemplate<String, String> redisTemplate;

    public RedisRepository(final RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Boolean lock(final Long key) {
        String generatedKey = generateKey(key);
        return redisTemplate
                .opsForValue()
                .setIfAbsent(generatedKey, "lock", Duration.ofMillis(3_000));
    }

    public Boolean unlock(final Long key) {
        String generatedKey = generateKey(key);
        return redisTemplate.delete(generatedKey);
    }

    public String generateKey(final Long key) {
        return key.toString();
    }
}
