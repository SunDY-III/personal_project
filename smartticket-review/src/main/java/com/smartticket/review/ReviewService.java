package com.smartticket.review;

import com.smartticket.common.*;
import com.smartticket.ticket.TicketService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

@Service
public class ReviewService {
    private final JdbcTemplate jdbc;
    private final TicketService ticketService;

    public ReviewService(JdbcTemplate jdbc, TicketService ticketService) {
        this.jdbc = jdbc;
        this.ticketService = ticketService;
    }

    @Transactional
    public Long create(Long ticketId, String toolName, String params, String riskLevel) {
        String requestHash = sha256(params);
        // 幂等检查：同一工单同一工具同一参数不重复创建
        var existing = jdbc.queryForList(
            "SELECT id FROM review_task WHERE ticket_id = ? AND tool_name = ? AND request_hash = ? AND status = 'PENDING'",
            ticketId, toolName, requestHash);
        if (!existing.isEmpty()) {
            return ((Number) existing.get(0).get("id")).longValue();
        }
        jdbc.update("INSERT INTO review_task (ticket_id, tool_name, request_params, request_hash, risk_level, status) VALUES (?,?,?,?,?,?)",
            ticketId, toolName, params, requestHash, riskLevel, ReviewStatus.PENDING.name());
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    @Transactional
    public void approve(Long reviewId, Long reviewerId, String comment) {
        int updated = jdbc.update("UPDATE review_task SET status = ?, reviewer_id = ?, review_comment = ?, reviewed_at = NOW() WHERE id = ? AND status = 'PENDING'",
            ReviewStatus.APPROVED.name(), reviewerId, comment, reviewId);
        if (updated == 0) throw new BizException(400, "审核任务不存在或已处理");

        var task = jdbc.queryForMap("SELECT ticket_id, tool_name FROM review_task WHERE id = ?", reviewId);
        Long ticketId = ((Number) task.get("ticket_id")).longValue();
        String toolName = (String) task.get("tool_name");

        // 模拟执行工具
        jdbc.update("INSERT INTO tool_call_log (ticket_id, run_id, tool_name, request_params, response_data, status, duration_ms) VALUES (?,?,?,?,?,?,?)",
            ticketId, "REVIEW_" + reviewId, toolName, "{}", "{\"result\":\"审核通过后执行成功\"}", "SUCCESS", 0);

        // 通过状态机驱动，保证工单当前为 WAITING_REVIEW
        ticketService.transition(ticketId, TicketStatus.RESOLVED, "REVIEWER", reviewerId, comment);
    }

    @Transactional
    public void reject(Long reviewId, Long reviewerId, String comment) {
        int updated = jdbc.update("UPDATE review_task SET status = ?, reviewer_id = ?, review_comment = ?, reviewed_at = NOW() WHERE id = ? AND status = 'PENDING'",
            ReviewStatus.REJECTED.name(), reviewerId, comment, reviewId);
        if (updated == 0) throw new BizException(400, "审核任务不存在或已处理");

        var task = jdbc.queryForMap("SELECT ticket_id FROM review_task WHERE id = ?", reviewId);
        Long ticketId = ((Number) task.get("ticket_id")).longValue();

        ticketService.transition(ticketId, TicketStatus.FAILED, "REVIEWER", reviewerId, "审核拒绝: " + comment);
    }

    public List<Map<String, Object>> list(int page, int size) {
        int offset = (page - 1) * size;
        return jdbc.queryForList("SELECT id, ticket_id, tool_name, risk_level, status, reviewer_id, review_comment, created_at, reviewed_at FROM review_task ORDER BY created_at DESC LIMIT ?, ?", offset, size);
    }

    public Map<String, Object> detail(Long id) {
        return jdbc.queryForMap("SELECT * FROM review_task WHERE id = ?", id);
    }

    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString().substring(0, 16);
        } catch (Exception e) { return Integer.toHexString(input.hashCode()); }
    }
}
