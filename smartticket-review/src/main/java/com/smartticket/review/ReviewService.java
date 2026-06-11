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
    }

    @Transactional
    public void reject(Long reviewId, Long reviewerId, String comment) {
        jdbc.update("UPDATE review_task SET status = ?, reviewer_id = ?, review_comment = ?, reviewed_at = NOW() WHERE id = ?",
            ReviewStatus.REJECTED.name(), reviewerId, comment, reviewId);
    }

    public List<Map<String, Object>> list() {
        return jdbc.queryForList("SELECT * FROM review_task ORDER BY created_at DESC");
    }

    public Map<String, Object> detail(Long id) {
        return jdbc.queryForMap("SELECT * FROM review_task WHERE id = ?", id);
    }
}
