package com.smartticket.sse;

import com.smartticket.common.BizException;
import com.smartticket.ticket.TicketService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/agent")
public class SseController {
    private final SseService sseService;
    private final TicketService ticketService;

    public SseController(SseService sseService, TicketService ticketService) {
        this.sseService = sseService;
        this.ticketService = ticketService;
    }

    @GetMapping(value = "/stream/{ticketId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable Long ticketId, HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("userId");
        String role = (String) req.getAttribute("role");
        // 校验用户有权订阅该工单
        ticketService.detail(ticketId, userId, role);
        return sseService.create(ticketId);
    }
}
