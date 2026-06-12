package com.smartticket.knowledge;

import com.smartticket.common.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@Service
public class DocumentParseService {
    private static final Logger log = LoggerFactory.getLogger(DocumentParseService.class);
    private static final Path UPLOAD_DIR = Paths.get("uploads/smartticket");

    private final JdbcTemplate jdbc;
    private final DocumentParseTaskService parseTaskService;

    public DocumentParseService(JdbcTemplate jdbc, DocumentParseTaskService parseTaskService) {
        this.jdbc = jdbc;
        this.parseTaskService = parseTaskService;
    }

    @Transactional
    public Map<String, Object> upload(String fileName, InputStream inputStream, String md5, Long userId) {
        var existing = jdbc.queryForList("SELECT id FROM knowledge_document WHERE file_md5 = ?", md5);
        if (!existing.isEmpty()) throw new BizException(400, "文档已存在");

        // 保存文件到本地磁盘
        try {
            Files.createDirectories(UPLOAD_DIR);
        } catch (IOException e) {
            throw new BizException(500, "创建上传目录失败");
        }
        String fileUrl = UPLOAD_DIR.resolve(fileName).toString();

        jdbc.update("INSERT INTO knowledge_document (file_name, file_url, file_md5, parse_status, created_by) VALUES (?,?,?,?,?)",
            fileName, fileUrl, md5, DocParseStatus.PENDING.name(), userId);
        Long docId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);

        // 读取字节并异步解析
        try {
            byte[] bytes = inputStream.readAllBytes();
            Files.write(Paths.get(fileUrl), bytes);
            parseTaskService.parseAsync(docId, fileName, bytes);
        } catch (Exception e) {
            throw new BizException(500, "保存文件失败: " + e.getMessage());
        }
        return Map.of("docId", docId, "status", "PENDING");
    }

    public java.util.List<Map<String, Object>> list() {
        return jdbc.queryForList("SELECT * FROM knowledge_document ORDER BY created_at DESC");
    }
}
