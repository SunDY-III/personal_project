package com.smartticket.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import java.time.Duration;

@Component
@RequiredArgsConstructor
public class CacheService {
    private final StringRedisTemplate redis;
    private final ObjectMapper mapper = new ObjectMapper();

    public String get(String key) { return redis.opsForValue().get(key); }

    public void set(String key, String value, Duration ttl) { redis.opsForValue().set(key, value, ttl); }

    public void set(String key, String value) { redis.opsForValue().set(key, value); }

    public long incr(String key, Duration window) {
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1) redis.expire(key, window);
        return count == null ? 0 : count;
    }

    public boolean allow(String key, int limit, Duration window) {
        return incr(key, window) <= limit;
    }

    public String questionHash(String question) {
        return Integer.toHexString(question.trim().toLowerCase().hashCode());
    }
}
