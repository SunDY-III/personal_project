package com.smartticket.ticket;

import com.smartticket.common.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.SecureRandom;
import java.util.*;

@Service
public class TicketService {
    private final JdbcTemplate jdbc;
    private static final SecureRandom RANDOM = new SecureRandom();

    public TicketService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public Map<String, Object> create(Long userId, String title, String content) {
        // 毫秒时间戳 + 纳秒 + 4位随机数
        String ticketNo = "TK" + System.currentTimeMillis() + System.nanoTime() % 10000 + String.format("%04d", RANDOM.nextInt(10000));
        jdbc.update("INSERT INTO ticket (ticket_no, user_id, title, content, status) VALUES (?,?,?,?,?)",
            ticketNo, userId, title, content, TicketStatus.CREATED.name());
        Long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        logFlow(id, null, TicketStatus.CREATED, "USER", userId, "用户提交工单");
        return Map.of("id", id, "ticketNo", ticketNo);
    }

    public List<Map<String, Object>> list(Long userId, String role, int page, int size) {
        int offset = (page - 1) * size;
        if ("USER".equals(role)) {
            return jdbc.queryForList("SELECT * FROM ticket WHERE user_id = ? ORDER BY created_at DESC LIMIT ?, ?", userId, offset, size);
        }
        return jdbc.queryForList("SELECT * FROM ticket ORDER BY created_at DESC LIMIT ?, ?", offset, size);
    }

    public Map<String, Object> detail(Long id, Long userId, String role) {
        if ("USER".equals(role)) checkOwnership(id, userId);
        return jdbc.queryForMap("SELECT * FROM ticket WHERE id = ?", id);
    }

    @Transactional
    public void transition(Long ticketId, TicketStatus to, String operatorType, Long operatorId, String remark) {
        String fromStatus = jdbc.queryForObject("SELECT status FROM ticket WHERE id = ?", String.class, ticketId);
        TicketStatus from = TicketStatus.valueOf(fromStatus);
        if (!TicketStateMachine.canTransit(from, to))
            throw new BizException(400, "非法状态流转: " + from + " -> " + to);
        jdbc.update("UPDATE ticket SET status = ?, updated_at = NOW() WHERE id = ?", to.name(), ticketId);
        logFlow(ticketId, from, to, operatorType, operatorId, remark);
    }

    @Transactional
    public void close(Long id, Long userId, String role) {
        if ("USER".equals(role)) checkOwnership(id, userId);
        transition(id, TicketStatus.CLOSED, role, userId, "关闭工单");
    }

    public List<Map<String, Object>> flowLogs(Long ticketId, Long userId, String role) {
        if ("USER".equals(role)) checkOwnership(ticketId, userId);
        return jdbc.queryForList("SELECT * FROM ticket_flow_log WHERE ticket_id = ? ORDER BY created_at", ticketId);
    }

    private void checkOwnership(Long ticketId, Long userId) {
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM ticket WHERE id = ? AND user_id = ?", Integer.class, ticketId, userId);
        if (count == null || count == 0) throw new BizException(403, "无权查看该工单");
    }

    private void logFlow(Long ticketId, TicketStatus from, TicketStatus to, String operatorType, Long operatorId, String remark) {
        jdbc.update("INSERT INTO ticket_flow_log (ticket_id, from_status, to_status, operator_type, operator_id, remark) VALUES (?,?,?,?,?,?)",
            ticketId, from == null ? null : from.name(), to.name(), operatorType, operatorId, remark);
    }
}
