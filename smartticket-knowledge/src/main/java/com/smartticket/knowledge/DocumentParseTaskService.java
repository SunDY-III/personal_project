package com.smartticket.knowledge;

import com.smartticket.common.DocParseStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.io.InputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentParseTaskService {
    private final JdbcTemplate jdbc;
    private final Tika tika = new Tika();
    private final OverlapTextSplitter splitter = new OverlapTextSplitter();
    private final EmbeddingService embeddingService;

    @Async("documentParseExecutor")
    public void parseAsync(Long docId, String fileName) {
        try {
            jdbc.update("UPDATE knowledge_document SET parse_status = ? WHERE id = ?",
                DocParseStatus.PARSING.name(), docId);
            String text;
            try (InputStream is = new java.net.URL("minio://smartticket/docs/" + fileName).openStream()) {
                text = tika.parseToString(is);
            }
            var chunks = splitter.split(text, docId);
            for (var chunk : chunks) {
                jdbc.update("INSERT INTO knowledge_chunk (doc_id, chunk_index, content) VALUES (?,?,?)",
                    docId, chunk.index(), chunk.content());
                Long chunkId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
                double[] vector = embeddingService.embed(chunk.content());
                embeddingService.saveVector(chunkId, docId, chunk.content(), vector);
                jdbc.update("UPDATE knowledge_chunk SET vector_id = ? WHERE id = ?", "vec_" + chunkId, chunkId);
            }
            jdbc.update("UPDATE knowledge_document SET parse_status = ? WHERE id = ?",
                DocParseStatus.READY.name(), docId);
            log.info("Document {} parsed successfully, {} chunks", docId, chunks.size());
        } catch (Exception e) {
            log.error("Document {} parse failed", docId, e);
            jdbc.update("UPDATE knowledge_document SET parse_status = ?, error_message = ? WHERE id = ?",
                DocParseStatus.FAILED.name(), e.getMessage(), docId);
        }
    }
}
