package com.smartticket.agent;

import com.smartticket.audit.AgentTraceRecorder;
import com.smartticket.cache.*;
import com.smartticket.common.*;
import com.smartticket.knowledge.EmbeddingService;
import com.smartticket.review.ReviewService;
import com.smartticket.sse.SseService;
import com.smartticket.ticket.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentService {
    private final JdbcTemplate jdbc;
    private final EmbeddingService embeddingService;
    private final ToolRiskGuard toolGuard;
    private final AgentTraceRecorder traceRecorder;
    private final TicketService ticketService;
    private final SseService sseService;
    private final ContextManager contextManager;
    private final CacheService cache;
    private final ReviewService reviewService;

    /** 同步问答（chat模式，不创建工单） */
    public Map<String, Object> chat(String sessionId, String question) {
        contextManager.append(sessionId, "user", question);
        // 缓存检查
        String hash = cache.questionHash(question);
        String cached = cache.get("rag:qa:" + hash);
        if (cached != null) { contextManager.append(sessionId, "assistant", cached); return Map.of("answer", cached, "cached", true); }
        // RAG检索
        var results = embeddingService.search(question, 5);
        if (results.isEmpty()) {
            String fallback = "当前知识库无法确认该问题，建议创建工单由人工客服处理。";
            contextManager.append(sessionId, "assistant", fallback);
            return Map.of("answer", fallback, "sources", List.of());
        }
        // 模拟LLM生成
        String answer = "根据知识库规则，" + question + " 的处理方式为：" + results.get(0).get("content").toString().substring(0, Math.min(80, results.get(0).get("content").toString().length()));
        cache.set("rag:qa:" + hash, answer, Duration.ofMinutes(30));
        contextManager.append(sessionId, "assistant", answer);
        return Map.of("answer", answer, "sources", results.stream().map(r -> "doc#" + r.get("doc_id")).toList());
    }

    /** 异步Agent分析工单 */
    @Async
    public void analyzeTicket(Long ticketId, String question, Long userId) {
        String runId = traceRecorder.startRun(ticketId, userId, question);
        try {
            ticketService.transition(ticketId, TicketStatus.ANALYZING, "AGENT", null, "Agent开始分析");
            sseService.send(ticketId, "START", "开始分析工单");

            // Step 1: 意图识别
            sseService.send(ticketId, "RAG_SEARCHING", "正在检索知识库");
            var ragResults = embeddingService.search(question, 5);
            traceRecorder.recordStep(runId, "RAG_SEARCH", question, ragResults, "SUCCESS", 0);

            // Step 2: 工具调用（模拟Agent决策）
            sseService.send(ticketId, "TOOL_CALLING", "正在查询订单和物流");
            Map<String, Object> orderResult = null;
            if (question.contains("订单") || question.contains("物流") || question.contains("发货")) {
                // 从问题中提取订单号（简化处理）
                String orderNo = extractOrderNo(question);
                if (orderNo != null) {
                    var r1 = toolGuard.execute("query_order", Map.of("orderNo", orderNo), ticketId, runId);
                    var r2 = toolGuard.execute("query_logistics", Map.of("orderNo", orderNo), ticketId, runId);
                    orderResult = Map.of("order", r1.data(), "logistics", r2.data());
                }
            }

            // Step 3: 退款检测
            boolean needReview = false;
            if (question.contains("退款") || question.contains("退货") || question.contains("坏了")) {
                sseService.send(ticketId, "TOOL_CALLING", "正在判断退款资格");
                String orderNo = extractOrderNo(question);
                var refundCheck = toolGuard.execute("check_refund_policy", Map.of("orderNo", orderNo != null ? orderNo : "unknown", "reason", question), ticketId, runId);
                var refundResult = toolGuard.execute("create_refund", Map.of("orderNo", orderNo != null ? orderNo : "unknown", "amount", 299.0, "reason", question), ticketId, runId);
                if (refundResult.needReview()) {
                    needReview = true;
                    sseService.send(ticketId, "WAITING_REVIEW", "退款申请已提交人工审核");
                    ticketService.transition(ticketId, TicketStatus.WAITING_REVIEW, "AGENT", null, "高风险退款操作，转人工审核");
                }
            }

            // Step 4: 生成最终回答
            sseService.send(ticketId, "ANSWERING", "正在生成处理建议");
            String finalAnswer = buildAnswer(question, ragResults, orderResult, needReview);
            traceRecorder.finishRun(runId, finalAnswer, "SUCCESS");

            if (!needReview) {
                ticketService.transition(ticketId, TicketStatus.RESOLVED, "AGENT", null, finalAnswer);
            }
            sseService.send(ticketId, "DONE", finalAnswer);

        } catch (Exception e) {
            log.error("Agent analysis failed for ticket {}", ticketId, e);
            traceRecorder.finishRun(runId, "分析失败：" + e.getMessage(), "FAILED");
            try { ticketService.transition(ticketId, TicketStatus.FAILED, "SYSTEM", null, "Agent分析异常"); }
            catch (Exception ex) { log.error("Failed to transition ticket", ex); }
            sseService.send(ticketId, "ERROR", "分析失败：" + e.getMessage());
        }
    }

    private String extractOrderNo(String text) {
        // 简化：查找ORD开头的订单号
        int idx = text.indexOf("ORD");
        if (idx >= 0) {
            int end = idx + 3;
            while (end < text.length() && (Character.isDigit(text.charAt(end)) || text.charAt(end) == '-')) end++;
            return text.substring(idx, end);
        }
        return null;
    }

    private String buildAnswer(String question, List<Map<String, Object>> ragResults, Map<String, Object> orderResult, boolean needReview) {
        StringBuilder sb = new StringBuilder();
        if (!ragResults.isEmpty()) sb.append("【知识库】").append(ragResults.get(0).get("content")).append("\n");
        if (orderResult != null) {
            sb.append("【订单信息】").append(orderResult.get("order")).append("\n");
            sb.append("【物流信息】").append(orderResult.get("logistics")).append("\n");
        }
        if (needReview) sb.append("【注意】该操作已提交人工审核，请耐心等待。");
        else sb.append("【建议】问题已自动处理完成。");
        return sb.toString();
    }
}
