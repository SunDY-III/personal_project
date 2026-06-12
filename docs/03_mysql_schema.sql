-- SmartTicket MySQL 建表（BCrypt密码）
CREATE TABLE IF NOT EXISTS sys_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL UNIQUE,
  password_hash VARCHAR(128) NOT NULL,
  role VARCHAR(32) NOT NULL DEFAULT 'USER',
  status TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- BCrypt hash of '123456': $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
INSERT IGNORE INTO sys_user (username, password_hash, role) VALUES
('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ADMIN'),
('staff', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'STAFF'),
('reviewer', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'REVIEWER'),
('user1', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'USER');

CREATE TABLE IF NOT EXISTS ticket (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  ticket_no VARCHAR(64) NOT NULL UNIQUE,
  user_id BIGINT NOT NULL,
  title VARCHAR(256) NOT NULL,
  content TEXT NOT NULL,
  intent_type VARCHAR(64),
  status VARCHAR(32) NOT NULL DEFAULT 'CREATED',
  agent_summary TEXT,
  solution TEXT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_user_id (user_id),
  INDEX idx_status (status)
);

CREATE TABLE IF NOT EXISTS ticket_flow_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  ticket_id BIGINT NOT NULL,
  from_status VARCHAR(32),
  to_status VARCHAR(32) NOT NULL,
  operator_type VARCHAR(32) NOT NULL,
  operator_id BIGINT,
  remark VARCHAR(512),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_ticket_id (ticket_id)
);

CREATE TABLE IF NOT EXISTS knowledge_document (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  file_name VARCHAR(256) NOT NULL,
  file_url VARCHAR(512) NOT NULL,
  file_md5 VARCHAR(64),
  parse_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  error_message VARCHAR(512),
  created_by BIGINT NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_parse_status (parse_status),
  UNIQUE KEY uk_file_md5 (file_md5)
);

CREATE TABLE IF NOT EXISTS knowledge_chunk (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  doc_id BIGINT NOT NULL,
  chunk_index INT NOT NULL,
  content TEXT NOT NULL,
  vector_id VARCHAR(128),
  metadata JSON,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_doc_id (doc_id),
  UNIQUE KEY uk_doc_chunk (doc_id, chunk_index)
);

CREATE TABLE IF NOT EXISTS tool_definition (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tool_name VARCHAR(128) NOT NULL UNIQUE,
  display_name VARCHAR(128) NOT NULL,
  risk_level VARCHAR(32) NOT NULL,
  required_permission VARCHAR(128),
  cache_ttl_seconds INT DEFAULT 0,
  enabled TINYINT NOT NULL DEFAULT 1,
  description VARCHAR(512)
);

INSERT IGNORE INTO tool_definition (tool_name, display_name, risk_level, cache_ttl_seconds, description) VALUES
('query_order', '查询订单', 'LOW', 300, '根据订单号查询订单信息'),
('query_logistics', '查询物流', 'LOW', 300, '根据订单号查询物流状态'),
('check_refund_policy', '退款资格判断', 'MEDIUM', 60, '判断订单是否满足退款条件'),
('create_ticket', '创建工单', 'LOW', 0, '创建新的客服工单'),
('create_refund', '创建退款申请', 'HIGH', 0, '创建退款申请（需人工审核）'),
('issue_coupon', '发放优惠券', 'HIGH', 0, '发放补偿优惠券（需人工审核）');

CREATE TABLE IF NOT EXISTS tool_call_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  ticket_id BIGINT,
  run_id VARCHAR(64),
  tool_name VARCHAR(128) NOT NULL,
  request_params JSON,
  response_data JSON,
  status VARCHAR(32) NOT NULL,
  duration_ms INT DEFAULT 0,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_ticket_id (ticket_id),
  INDEX idx_run_id (run_id)
);

CREATE TABLE IF NOT EXISTS review_task (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  ticket_id BIGINT NOT NULL,
  tool_name VARCHAR(128) NOT NULL,
  request_params JSON,
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  reviewer_id BIGINT,
  review_comment VARCHAR(512),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  reviewed_at DATETIME,
  INDEX idx_ticket_id (ticket_id),
  INDEX idx_status (status),
  UNIQUE KEY uk_ticket_tool (ticket_id, tool_name)
);

CREATE TABLE IF NOT EXISTS agent_run (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  run_id VARCHAR(64) NOT NULL UNIQUE,
  ticket_id BIGINT,
  user_id BIGINT NOT NULL,
  input_text TEXT NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'RUNNING',
  final_answer TEXT,
  started_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  ended_at DATETIME,
  INDEX idx_ticket_id (ticket_id),
  INDEX idx_user_id (user_id)
);

CREATE TABLE IF NOT EXISTS agent_step (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  run_id VARCHAR(64) NOT NULL,
  step_name VARCHAR(128) NOT NULL,
  input_data JSON,
  output_data JSON,
  status VARCHAR(32) NOT NULL,
  duration_ms INT DEFAULT 0,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_run_id (run_id)
);
