# java-ai

这是一个本地 AI 客服学习项目，采用 Spring Boot 微服务架构，支持多对话、上下文、RAG 本地知识库与本地 LLM 调用。

**架构说明**
1. `services/chat-service`（端口 8081）
1. 聊天会话与消息持久化（MySQL）
1. 调用本地 LLM（llama 服务）回答问题
1. 调用 `rag-service` 检索本地知识库
1. 内置简单 Web UI，支持多会话切换与消息发送
2. `services/rag-service`（端口 8082）
1. 本地文档上传与解析（PDF/Word/Excel）
1. 本地向量化（本地向量模型服务）
1. 向量数据库（Qdrant）存储与检索
1. 支持按文档 ID 删除
3. `libs/ai-common`
1. 两个服务共用的 DTO 定义
4. `legacy-webapp`
1. 原始 Maven Webapp 保留作为参考

**关键配置说明**
1. 本地 LLM API 地址与模型通过配置文件设置（通过 `llama` 启动，模型为 `qwen3.5`）：
1. `services/chat-service/src/main/resources/application.yml`
1. `llm.base-url`、`llm.model`
2. RAG 向量化模型与 Qdrant 地址：
1. `services/rag-service/src/main/resources/application.yml`
1. `embedding.base-url`、`embedding.model`、`rag.qdrant-base-url`（默认 `BAAI/bge-base-zh-v1.5`）
1. 向量服务默认监听 `http://localhost:8090/embeddings`

**启动步骤**
1. 启动基础设施（MySQL、Qdrant）：
1. `docker compose up -d`
2. 启动 llama 服务并加载模型：
1. 确保本地 `llama` 服务提供 OpenAI 兼容接口（`/v1/chat/completions`）
1. 模型配置为 `qwen3.5`（可在 `application.yml` 中调整）
3. 启动本地向量模型服务（示例，Python）：
1. `cd tools/embedding-service`
1. `python -m venv .venv`
1. `. .venv/bin/activate`
1. `pip install -r requirements.txt`
1. `uvicorn app:app --host 0.0.0.0 --port 8090`
3. 启动服务：
1. `./mvnw -pl services/rag-service spring-boot:run`
1. `./mvnw -pl services/chat-service spring-boot:run`
4. 访问页面：
1. `http://localhost:8081`

**RAG 使用说明（本地向量模型方案）**
1. 方案说明
1. 文档在 `rag-service` 本地解析（Apache Tika）
1. 使用本地向量模型 `BAAI/bge-base-zh-v1.5` 在本地生成向量
1. 向量存储与检索使用 Qdrant（本地）
1. 检索出的相关片段拼接到提示词中，通过本地 LLM API（llama）生成答案

**RAG 使用说明**
1. 页面上传（推荐）
1. 进入 `http://localhost:8081`
1. 在 “Knowledge Base” 区域上传 PDF/Word/Excel 文件
1. 上传后会返回文档 ID，用于删除
2. API 上传
```
curl -X POST http://localhost:8082/api/knowledge/upload \
  -F "files=@/path/to/your.pdf" \
  -F "files=@/path/to/your.docx"
```
3. API 删除
```
curl -X POST http://localhost:8082/api/knowledge/delete \
  -H "Content-Type: application/json" \
  -d '{"documentId":"<DOC_ID>"}'
```
4. API 检索（测试）
```
curl -X POST http://localhost:8082/api/knowledge/search \
  -H "Content-Type: application/json" \
  -d '{"query":"What is the return policy?","topK":3}'
```

**聊天 API**
1. 创建会话：`POST /api/chats`
2. 列出会话：`GET /api/chats`
3. 获取消息：`GET /api/chats/{chatId}/messages`
4. 发送消息：`POST /api/chats/{chatId}/messages`

**核心代码说明（入口与流程）**
1. Chat 服务入口
1. `services/chat-service/src/main/java/org/example/chat/ChatServiceApplication.java`
2. Chat 核心编排（上下文 + RAG + LLM）
1. `services/chat-service/src/main/java/org/example/chat/ChatOrchestrator.java`
3. RAG 服务入口
1. `services/rag-service/src/main/java/org/example/rag/RagServiceApplication.java`
4. RAG 核心编排（分块 + 向量化 + 存储/检索）
1. `services/rag-service/src/main/java/org/example/rag/RagOrchestrator.java`
