package com.smartticket.audit;

import com.smartticket.common.BizException;
import com.smartticket.common.R;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/agent/runs")
public class AuditController {
    private final AgentTraceRecorder traceRecorder;
    public AuditController(AgentTraceRecorder traceRecorder) {
        this.traceRecorder = traceRecorder;
    }

    @GetMapping
    public R<?> list(@RequestAttribute("role") String role,
            @RequestAttribute(value = "userId", required = false) Long userId) {
        checkAuditRole(role);
        return R.ok(traceRecorder.getRunsByRole(role, userId));
    }

    @GetMapping("/{runId}/steps")
    public R<?> steps(@PathVariable String runId,
            @RequestAttribute("role") String role,
            @RequestAttribute(value = "userId", required = false) Long userId) {
        checkAuditRole(role);
        return R.ok(traceRecorder.getStepsByRole(runId, role, userId));
    }

    private void checkAuditRole(String role) {
        if (!"ADMIN".equals(role) && !"STAFF".equals(role))
            throw new BizException(403, "无审计查看权限");
    }
}
