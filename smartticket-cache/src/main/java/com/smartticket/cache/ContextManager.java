package com.smartticket.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.util.*;

@Component
public class ContextManager {
    private final StringRedisTemplate redis;
    private final TokenEstimator tokenEstimator;
    private final ObjectMapper mapper = new ObjectMapper();
    public ContextManager(StringRedisTemplate redis, TokenEstimator tokenEstimator) {
        this.redis = redis;
        this.tokenEstimator = tokenEstimator;
    }
    private static final int MAX_TOKENS = 3200; // 历史对话预算
    private static final int RECENT_ROUNDS = 3;

    public void save(String sessionId, List<Map<String, Object>> messages) {
        try {
            redis.opsForValue().set("agent:context:" + sessionId, mapper.writeValueAsString(messages), Duration.ofHours(2));
        } catch (JsonProcessingException e) { throw new RuntimeException(e); }
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> load(String sessionId) {
        String json = redis.opsForValue().get("agent:context:" + sessionId);
        if (json == null) return new ArrayList<>();
        try { return mapper.readValue(json, List.class); } catch (JsonProcessingException e) { return new ArrayList<>(); }
    }

    public void append(String sessionId, String role, String content) {
        List<Map<String, Object>> msgs = load(sessionId);
        msgs.add(Map.of("role", role, "content", content));
        trim(msgs);
        save(sessionId, msgs);
    }

    void trim(List<Map<String, Object>> msgs) {
        if (msgs.size() <= RECENT_ROUNDS * 2) return;
        int totalTokens = msgs.stream().mapToInt(m -> tokenEstimator.estimate((String) m.get("content"))).sum();
        while (totalTokens > MAX_TOKENS && msgs.size() > RECENT_ROUNDS * 2) {
            Map<String, Object> removed = msgs.remove(0);
            totalTokens -= tokenEstimator.estimate((String) removed.get("content"));
        }
    }
}
