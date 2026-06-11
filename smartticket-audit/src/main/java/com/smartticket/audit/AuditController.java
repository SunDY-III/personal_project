package com.smartticket.audit;

import com.smartticket.common.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/agent/runs")
@RequiredArgsConstructor
public class AuditController {
    private final AgentTraceRecorder traceRecorder;

    @GetMapping
    public R<?> list() { return R.ok(traceRecorder.getRuns()); }

    @GetMapping("/{runId}/steps")
    public R<?> steps(@PathVariable String runId) { return R.ok(traceRecorder.getSteps(runId)); }
}
