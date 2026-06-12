package com.smartticket.audit;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class AgentTraceRecorder {
    private final JdbcTemplate jdbc;
    public AgentTraceRecorder(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public String startRun(Long ticketId, Long userId, String input) {
        String runId = "RUN" + System.currentTimeMillis();
        jdbc.update("INSERT INTO agent_run (run_id, ticket_id, user_id, input_text, status) VALUES (?,?,?,?,?)",
            runId, ticketId, userId, input, "RUNNING");
        return runId;
    }

    @Transactional
    public void recordStep(String runId, String stepName, Object input, Object output, String status, long durationMs) {
        jdbc.update("INSERT INTO agent_step (run_id, step_name, input_data, output_data, status, duration_ms) VALUES (?,?,?,?,?,?)",
            runId, stepName, toJson(input), toJson(output), status, durationMs);
    }

    @Transactional
    public void finishRun(String runId, String finalAnswer, String status) {
        jdbc.update("UPDATE agent_run SET final_answer = ?, status = ?, ended_at = NOW() WHERE run_id = ?",
            finalAnswer, status, runId);
    }

    @Transactional
    public void logToolCall(Long ticketId, String runId, String toolName, Object params, Object result, String status, long durationMs) {
        jdbc.update("INSERT INTO tool_call_log (ticket_id, run_id, tool_name, request_params, response_data, status, duration_ms) VALUES (?,?,?,?,?,?,?)",
            ticketId, runId, toolName, toJson(params), toJson(result), status, durationMs);
    }

    public List<Map<String, Object>> getRuns() {
        return jdbc.queryForList("SELECT id, run_id, ticket_id, user_id, status, started_at, ended_at FROM agent_run ORDER BY started_at DESC LIMIT 20");
    }

    public List<Map<String, Object>> getRunsByRole(String role, Long userId) {
        if ("ADMIN".equals(role) || "STAFF".equals(role))
            return getRuns();
        return jdbc.queryForList("SELECT id, run_id, ticket_id, user_id, status, started_at, ended_at FROM agent_run WHERE user_id = ? ORDER BY started_at DESC LIMIT 20", userId);
    }

    public List<Map<String, Object>> getSteps(String runId) {
        return jdbc.queryForList("SELECT * FROM agent_step WHERE run_id = ? ORDER BY created_at", runId);
    }

    public List<Map<String, Object>> getStepsByRole(String runId, String role, Long userId) {
        if ("ADMIN".equals(role) || "STAFF".equals(role))
            return getSteps(runId);
        // Verify user owns this run
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM agent_run WHERE run_id = ? AND user_id = ?", Integer.class, runId, userId);
        if (count == null || count == 0) return List.of();
        return getSteps(runId);
    }

    private String toJson(Object obj) {
        try { return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(obj); }
        catch (Exception e) { return "{}"; }
    }
}
