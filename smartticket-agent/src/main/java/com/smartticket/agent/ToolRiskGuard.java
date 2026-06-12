package com.smartticket.agent;

import com.smartticket.audit.AgentTraceRecorder;
import com.smartticket.cache.CacheService;
import com.smartticket.common.ToolRiskLevel;
import com.smartticket.mock.*;
import com.smartticket.review.ReviewService;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.util.*;

@Component
public class ToolRiskGuard {
    private final ToolRegistry toolRegistry;
    private final ReviewService reviewService;
    private final AgentTraceRecorder traceRecorder;
    private final CacheService cache;
    private final MockOrderService orderService;
    private final MockLogisticsService logisticsService;
    private final MockRefundService refundService;
    public ToolRiskGuard(ToolRegistry toolRegistry, ReviewService reviewService, AgentTraceRecorder traceRecorder, CacheService cache, MockOrderService orderService, MockLogisticsService logisticsService, MockRefundService refundService) {
        this.toolRegistry = toolRegistry;
        this.reviewService = reviewService;
        this.traceRecorder = traceRecorder;
        this.cache = cache;
        this.orderService = orderService;
        this.logisticsService = logisticsService;
        this.refundService = refundService;
    }

    public ToolResult execute(String toolName, Map<String, Object> params, Long ticketId, String runId) {
        long start = System.currentTimeMillis();

        // 检查工具是否启用
        if (!toolRegistry.isEnabled(toolName)) {
            traceRecorder.logToolCall(ticketId, runId, toolName, params, "TOOL_DISABLED", "REJECTED", System.currentTimeMillis() - start);
            return new ToolResult("工具不可用：" + toolName, false, false);
        }

        ToolRiskLevel risk = toolRegistry.getRiskLevel(toolName);

        // 检查缓存
        if (risk == ToolRiskLevel.LOW) {
            String cacheKey = "tool:" + toolName + ":" + params.hashCode();
            String cached = cache.get(cacheKey);
            if (cached != null) {
                traceRecorder.logToolCall(ticketId, runId, toolName, params, cached, "SUCCESS_CACHED", System.currentTimeMillis() - start);
                return new ToolResult(cached, true, false);
            }
        }

        // HIGH风险 → 转人工审核
        if (risk == ToolRiskLevel.HIGH) {
            String paramsJson = toJson(params);
            Long reviewId = reviewService.create(ticketId, toolName, paramsJson, "HIGH");
            traceRecorder.logToolCall(ticketId, runId, toolName, params, "NEED_REVIEW:" + reviewId, "NEED_REVIEW", System.currentTimeMillis() - start);
            return new ToolResult("该操作涉及资金或权益，已转人工审核（审核ID：" + reviewId + "）", false, true);
        }

        // 执行工具
        try {
            Object result = invokeTool(toolName, params);
            String resultJson = toJson(result);
            traceRecorder.logToolCall(ticketId, runId, toolName, params, resultJson, "SUCCESS", System.currentTimeMillis() - start);
            // 缓存低风险结果
            if (risk == ToolRiskLevel.LOW) {
                int ttl = toolRegistry.getCacheTtl(toolName);
                if (ttl > 0) cache.set("tool:" + toolName + ":" + params.hashCode(), resultJson, Duration.ofSeconds(ttl));
            }
            return new ToolResult(resultJson, false, false);
        } catch (Exception e) {
            traceRecorder.logToolCall(ticketId, runId, toolName, params, e.getMessage(), "FAILED", System.currentTimeMillis() - start);
            return new ToolResult("工具调用失败：" + e.getMessage(), false, false);
        }
    }

    private Object invokeTool(String toolName, Map<String, Object> params) {
        return switch (toolName) {
            case "query_order" -> orderService.queryOrder((String) params.get("orderNo"));
            case "query_logistics" -> logisticsService.queryLogistics((String) params.get("orderNo"));
            case "check_refund_policy" -> refundService.checkRefundPolicy((String) params.get("orderNo"), (String) params.get("reason"));
            case "create_ticket" -> Map.of("ticketId", params.getOrDefault("ticketId", 0), "status", "CREATED");
            case "create_refund" -> refundService.createRefund((String) params.get("orderNo"), ((Number) params.getOrDefault("amount", 0)).doubleValue(), (String) params.get("reason"));
            case "issue_coupon" -> {
                Object uid = params.get("userId");
                if (uid == null) throw new IllegalArgumentException("缺少参数 userId");
                Object amt = params.getOrDefault("amount", 0);
                yield refundService.issueCoupon(((Number) uid).longValue(),
                    ((Number) amt).doubleValue(), (String) params.getOrDefault("reason", ""));
            }
            default -> throw new IllegalArgumentException("Unknown tool: " + toolName);
        };
    }

    private String toJson(Object obj) {
        try { return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(obj); }
        catch (Exception e) { return "{}"; }
    }

    public record ToolResult(String data, boolean cached, boolean needReview) {}
}
