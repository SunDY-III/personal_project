# SmartTicket — 企业智能工单 Agent 平台

面向企业客服场景的智能工单处理平台，结合 RAG 知识库与 LangChain4j Agent 工具调用。

## 快速启动

```bash
# 1. 启动基础依赖（ES 可选，可注释掉）
docker compose up -d mysql redis postgres minio

# 2. 初始化 MySQL（自动执行 docs/03_mysql_schema.sql）
# 3. 初始化 pgvector
docker exec -i smartticket-postgres psql -U postgres -d smartticket_vector < docs/04_pgvector_schema.sql

# 4. 配置 LLM API Key
export LLM_API_KEY=your_api_key
export LLM_BASE_URL=https://api.openai.com/v1

# 5. 启动后端
mvn clean package -DskipTests
java -jar smartticket-server/target/smartticket-server-1.0.0-SNAPSHOT.jar

# 6. 访问接口文档
# http://localhost:8080/swagger-ui.html
```

## 技术栈

| 组件 | 用途 |
|------|------|
| Spring Boot 3 | 主框架 |
| MyBatis-Plus | ORM |
| MySQL | 业务数据库 |
| PostgreSQL + pgvector | 向量存储与语义检索 |
| Redis | 缓存、上下文、限流 |
| MinIO | 文档存储 |
| LangChain4j | AI Agent 框架 |
| Apache Tika | 文档解析 |
| SseEmitter | 流式推送 |
| Docker Compose | 本地部署 |

## 项目结构

```
smartticket/
├── smartticket-common/      # 统一返回、异常、枚举
├── smartticket-auth/        # JWT 登录鉴权
├── smartticket-ticket/      # 工单 CRUD + 状态机
├── smartticket-knowledge/   # RAG 管道（上传/解析/切片/向量/检索）
├── smartticket-agent/       # Agent 编排 + ToolRegistry + 风险分级
├── smartticket-review/      # 人工审核
├── smartticket-audit/       # Agent Trace 审计
├── smartticket-cache/       # Redis 缓存 + 上下文管理
├── smartticket-sse/         # SseEmitter 流式推送
├── smartticket-mock/        # 模拟订单/物流/退款系统
└── smartticket-server/      # 启动类 + 配置
```

## API 样例

```http
### 登录
POST /api/auth/login
{"username": "admin", "password": "123456"}

### 创建工单
POST /api/tickets
Authorization: Bearer <token>
{"title": "订单迟迟未发货", "content": "我的订单 ORD001 为什么还没发货？"}

### RAG 问答
POST /api/knowledge/ask
Authorization: Bearer <token>
{"question": "七天无理由退款规则是什么？"}

### Agent 对话
POST /api/agent/chat
Authorization: Bearer <token>
{"sessionId": "S001", "question": "订单 ORD001 的耳机坏了，我想退款"}

### SSE 流式推送
GET /api/agent/stream/1
Authorization: Bearer <token>
Accept: text/event-stream

### 审核通过
POST /api/reviews/1/approve
Authorization: Bearer <token>
{"comment": "同意创建退款申请"}

### Agent Trace
GET /api/agent/runs/RUN001/steps
Authorization: Bearer <token>
```
