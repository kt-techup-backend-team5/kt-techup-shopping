package com.kt.service;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MailRedisService {

    private final StringRedisTemplate redisTemplate;

    public String getData(String key) {
        ValueOperations<String, String> ops = redisTemplate.opsForValue();
        return ops.get(key);
    }

    public void setDataExpire(String key, String value, long duration) {
        ValueOperations<String, String> ops = redisTemplate.opsForValue();
        Duration expire = Duration.ofSeconds(duration);
        ops.set(key, value, expire);
    }

    public void deleteData(String key) {
        redisTemplate.delete(key);
    }
}