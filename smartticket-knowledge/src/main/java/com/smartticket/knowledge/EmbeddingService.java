package com.smartticket.knowledge;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmbeddingService {
    private final JdbcTemplate jdbc;
    private final JdbcTemplate pgJdbc; // pgvector

    public EmbeddingService(JdbcTemplate jdbc, @org.springframework.beans.factory.annotation.Qualifier("postgresJdbcTemplate") JdbcTemplate pgJdbc) {
        this.jdbc = jdbc;
        this.pgJdbc = pgJdbc;
    }

    /** 模拟Embedding - 实际项目调用 OpenAI/本地 Embedding API */
    public double[] embed(String text) {
        // 简化：用文本哈希生成伪向量
        int dim = 1536;
        double[] vec = new double[dim];
        int hash = text.hashCode();
        for (int i = 0; i < dim; i++) {
            vec[i] = Math.sin(hash * (i + 1) * 0.001) * 0.01;
        }
        return vec;
    }

    /** 保存向量到pgvector */
    public void saveVector(Long chunkId, Long docId, String content, double[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(vector[i]);
        }
        sb.append("]");
        pgJdbc.update("INSERT INTO knowledge_vector (chunk_id, doc_id, content, embedding) VALUES (?,?,?,?::vector)",
            chunkId, docId, content, sb.toString());
    }

    /** 向量相似度检索 */
    public java.util.List<java.util.Map<String, Object>> search(String queryText, int topK) {
        double[] queryVec = embed(queryText);
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < queryVec.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(queryVec[i]);
        }
        sb.append("]");
        return pgJdbc.queryForList(
            "SELECT chunk_id, doc_id, content, 1 - (embedding <=> ?::vector) AS similarity FROM knowledge_vector ORDER BY embedding <=> ?::vector LIMIT ?",
            sb.toString(), sb.toString(), topK);
    }
}
