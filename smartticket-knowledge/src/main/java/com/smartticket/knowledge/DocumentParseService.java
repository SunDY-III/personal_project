package com.smartticket.knowledge;

import com.smartticket.cache.CacheService;
import com.smartticket.common.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentParseService {
    private final JdbcTemplate jdbc;
    private final OverlapTextSplitter splitter = new OverlapTextSplitter();
    private final Tika tika = new Tika();
    private final EmbeddingService embeddingService;
    private final CacheService cache;

    @Transactional
    public Map<String, Object> upload(String fileName, InputStream inputStream, String md5, Long userId) {
        // MD5去重
        var existing = jdbc.queryForList("SELECT id FROM knowledge_document WHERE file_md5 = ?", md5);
        if (!existing.isEmpty()) throw new BizException(400, "文档已存在");
        String fileUrl = "minio://smartticket/docs/" + fileName;
        jdbc.update("INSERT INTO knowledge_document (file_name, file_url, file_md5, parse_status, created_by) VALUES (?,?,?,?,?)",
            fileName, fileUrl, md5, DocParseStatus.PENDING.name(), userId);
        Long docId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        // 异步解析
        parseAsync(docId, fileName);
        return Map.of("docId", docId, "status", "PENDING");
    }

    @Async("documentParseExecutor")
    public void parseAsync(Long docId, String fileName) {
        try {
            jdbc.update("UPDATE knowledge_document SET parse_status = ? WHERE id = ?", DocParseStatus.PARSING.name(), docId);
            // 从MinIO下载原始文件，通过Tika提取文本内容
            String text;
            try (InputStream is = new java.net.URL(fileUrl).openStream()) {
                text = tika.parseToString(is);
            }
            var chunks = splitter.split(text, docId);
            for (var chunk : chunks) {
                jdbc.update("INSERT INTO knowledge_chunk (doc_id, chunk_index, content) VALUES (?,?,?)",
                    docId, chunk.index(), chunk.content());
                Long chunkId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
                // 生成向量并写入pgvector（实际调用Embedding API）
                double[] vector = embeddingService.embed(chunk.content());
                embeddingService.saveVector(chunkId, docId, chunk.content(), vector);
                jdbc.update("UPDATE knowledge_chunk SET vector_id = ? WHERE id = ?", "vec_" + chunkId, chunkId);
            }
            jdbc.update("UPDATE knowledge_document SET parse_status = ? WHERE id = ?", DocParseStatus.READY.name(), docId);
            log.info("Document {} parsed successfully, {} chunks", docId, chunks.size());
        } catch (Exception e) {
            log.error("Document {} parse failed", docId, e);
            jdbc.update("UPDATE knowledge_document SET parse_status = ? WHERE id = ?", DocParseStatus.FAILED.name(), docId);
        }
    }

    public List<Map<String, Object>> list() {
        return jdbc.queryForList("SELECT * FROM knowledge_document ORDER BY created_at DESC");
    }
}
