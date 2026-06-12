package com.smartticket.review;

import com.smartticket.common.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final JdbcTemplate jdbc;

    @Transactional
    public Long create(Long ticketId, String toolName, String params, String riskLevel) {
        jdbc.update("INSERT INTO review_task (ticket_id, tool_name, request_params, risk_level, status) VALUES (?,?,?,?,?)",
            ticketId, toolName, params, riskLevel, ReviewStatus.PENDING.name());
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    @Transactional
    public void approve(Long reviewId, Long reviewerId, String comment) {
        jdbc.update("UPDATE review_task SET status = ?, reviewer_id = ?, review_comment = ?, reviewed_at = NOW() WHERE id = ?",
            ReviewStatus.APPROVED.name(), reviewerId, comment, reviewId);

        // 驱动工单状态
        var task = jdbc.queryForMap("SELECT ticket_id, tool_name FROM review_task WHERE id = ?", reviewId);
        Long ticketId = ((Number) task.get("ticket_id")).longValue();
        String toolName = (String) task.get("tool_name");

        // 模拟执行工具
        jdbc.update("INSERT INTO tool_call_log (ticket_id, run_id, tool_name, request_params, response_data, status, duration_ms) VALUES (?,?,?,?,?,?,?)",
            ticketId, "REVIEW_" + reviewId, toolName, "{}", "{\"result\":\"审核通过后执行成功\"}", "SUCCESS", 0);

        // 更新工单状态为已解决
        jdbc.update("UPDATE ticket SET status = ?, updated_at = NOW() WHERE id = ?",
            TicketStatus.RESOLVED.name(), ticketId);
        jdbc.update("INSERT INTO ticket_flow_log (ticket_id, from_status, to_status, operator_type, operator_id, remark) VALUES (?,?,?,?,?,?)",
            ticketId, TicketStatus.WAITING_REVIEW.name(), TicketStatus.RESOLVED.name(), "REVIEWER", reviewerId, comment);
    }

    @Transactional
    public void reject(Long reviewId, Long reviewerId, String comment) {
        jdbc.update("UPDATE review_task SET status = ?, reviewer_id = ?, review_comment = ?, reviewed_at = NOW() WHERE id = ?",
            ReviewStatus.REJECTED.name(), reviewerId, comment, reviewId);

        var task = jdbc.queryForMap("SELECT ticket_id FROM review_task WHERE id = ?", reviewId);
        Long ticketId = ((Number) task.get("ticket_id")).longValue();

        jdbc.update("UPDATE ticket SET status = ?, updated_at = NOW() WHERE id = ?",
            TicketStatus.FAILED.name(), ticketId);
        jdbc.update("INSERT INTO ticket_flow_log (ticket_id, from_status, to_status, operator_type, operator_id, remark) VALUES (?,?,?,?,?,?)",
            ticketId, TicketStatus.WAITING_REVIEW.name(), TicketStatus.FAILED.name(), "REVIEWER", reviewerId, "审核拒绝: " + comment);
    }

    public List<Map<String, Object>> list() {
        return jdbc.queryForList("SELECT * FROM review_task ORDER BY created_at DESC");
    }

    public Map<String, Object> detail(Long id) {
        return jdbc.queryForMap("SELECT * FROM review_task WHERE id = ?", id);
    }
}
