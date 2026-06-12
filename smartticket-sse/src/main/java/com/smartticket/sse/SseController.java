package com.smartticket.sse;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/agent")
public class SseController {
    private final SseService sseService;
    public SseController(SseService sseService) {
        this.sseService = sseService;
    }

    @GetMapping(value = "/stream/{ticketId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable Long ticketId) {
        return sseService.create(ticketId);
    }
}
