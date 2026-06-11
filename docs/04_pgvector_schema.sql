-- SmartTicket pgvector 建表
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS knowledge_vector (
  id BIGSERIAL PRIMARY KEY,
  chunk_id BIGINT NOT NULL,
  doc_id BIGINT NOT NULL,
  content TEXT NOT NULL,
  embedding vector(1536),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_knowledge_vector_hnsw
ON knowledge_vector
USING hnsw (embedding vector_cosine_ops);
