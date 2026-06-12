package com.smartticket.sse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SseService {
    private static final Logger log = LoggerFactory.getLogger(SseService.class);
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter create(Long ticketId) {
        SseEmitter emitter = new SseEmitter(60_000L);
        emitters.put(ticketId, emitter);
        emitter.onCompletion(() -> emitters.remove(ticketId));
        emitter.onTimeout(() -> emitters.remove(ticketId));
        emitter.onError(e -> emitters.remove(ticketId));
        return emitter;
    }

    public void send(Long ticketId, String event, String data) {
        SseEmitter emitter = emitters.get(ticketId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().name(event).data(data));
            } catch (IOException e) {
                emitters.remove(ticketId);
            }
        }
    }

    public boolean hasEmitter(Long ticketId) { return emitters.containsKey(ticketId); }
}
