package com.smartticket.knowledge;

import com.smartticket.common.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.InputStream;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentParseService {
    private final JdbcTemplate jdbc;
    private final DocumentParseTaskService parseTaskService;

    @Transactional
    public Map<String, Object> upload(String fileName, InputStream inputStream, String md5, Long userId) {
        var existing = jdbc.queryForList("SELECT id FROM knowledge_document WHERE file_md5 = ?", md5);
        if (!existing.isEmpty()) throw new BizException(400, "文档已存在");
        String fileUrl = "minio://smartticket/docs/" + fileName;
        jdbc.update("INSERT INTO knowledge_document (file_name, file_url, file_md5, parse_status, created_by) VALUES (?,?,?,?,?)",
            fileName, fileUrl, md5, DocParseStatus.PENDING.name(), userId);
        Long docId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        parseTaskService.parseAsync(docId, fileName);
        return Map.of("docId", docId, "status", "PENDING");
    }

    public java.util.List<Map<String, Object>> list() {
        return jdbc.queryForList("SELECT * FROM knowledge_document ORDER BY created_at DESC");
    }
}
