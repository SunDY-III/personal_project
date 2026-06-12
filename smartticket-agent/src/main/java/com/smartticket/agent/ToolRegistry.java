package com.smartticket.agent;

import com.smartticket.common.ToolRiskLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
@RequiredArgsConstructor
public class ToolRegistry {
    private final JdbcTemplate jdbc;

    public ToolRiskLevel getRiskLevel(String toolName) {
        var rows = jdbc.queryForList("SELECT risk_level FROM tool_definition WHERE tool_name = ? AND enabled = 1", toolName);
        if (rows.isEmpty()) return ToolRiskLevel.HIGH;
        return ToolRiskLevel.valueOf((String) rows.get(0).get("risk_level"));
    }

    public boolean isEnabled(String toolName) {
        var rows = jdbc.queryForList("SELECT enabled FROM tool_definition WHERE tool_name = ?", toolName);
        return !rows.isEmpty() && ((Number) rows.get(0).get("enabled")).intValue() == 1;
    }

    public int getCacheTtl(String toolName) {
        var rows = jdbc.queryForList("SELECT cache_ttl_seconds FROM tool_definition WHERE tool_name = ?", toolName);
        if (rows.isEmpty()) return 0;
        Object ttl = rows.get(0).get("cache_ttl_seconds");
        return ttl == null ? 0 : ((Number) ttl).intValue();
    }
}
