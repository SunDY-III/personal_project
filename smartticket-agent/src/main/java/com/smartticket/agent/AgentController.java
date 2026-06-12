package com.smartticket.agent;

import com.smartticket.common.R;
import com.smartticket.ticket.TicketService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentController {
    private final AgentService agentService;
    private final TicketService ticketService;

    @PostMapping("/chat")
    public R<?> chat(@RequestBody Map<String, String> body) {
        String question = body.get("question");
        if (question == null || question.isBlank())
            throw new com.smartticket.common.BizException(400, "问题不能为空");
        String sessionId = body.getOrDefault("sessionId", "S" + System.currentTimeMillis());
        return R.ok(agentService.chat(sessionId, question));
    }

    @PostMapping("/analyze-ticket/{ticketId}")
    public R<?> analyze(@PathVariable Long ticketId, HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("userId");
        var ticket = ticketService.detail(ticketId);
        agentService.analyzeTicket(ticketId, (String) ticket.get("content"), userId);
        return R.ok(Map.of("status", "ANALYZING"));
    }
}
