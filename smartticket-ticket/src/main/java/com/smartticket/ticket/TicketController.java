package com.smartticket.ticket;

import com.smartticket.common.R;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {
    private final TicketService ticketService;

    @PostMapping
    public R<Map<String, Object>> create(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("userId");
        String title = (String) body.get("title");
        String content = (String) body.get("content");
        return R.ok(ticketService.create(userId, title, content));
    }

    @GetMapping
    public R<List<Map<String, Object>>> list(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestAttribute("userId") Long userId,
        @RequestAttribute("role") String role) {
        return R.ok(ticketService.list(userId, role, page, size));
    }

    @GetMapping("/{id}")
    public R<Map<String, Object>> detail(@PathVariable Long id) {
        return R.ok(ticketService.detail(id));
    }

    @PostMapping("/{id}/close")
    public R<?> close(@PathVariable Long id, @RequestAttribute("userId") Long userId) {
        ticketService.close(id, userId);
        return R.ok();
    }

    @GetMapping("/{id}/flow-logs")
    public R<List<Map<String, Object>>> flowLogs(@PathVariable Long id) {
        return R.ok(ticketService.flowLogs(id));
    }
}
