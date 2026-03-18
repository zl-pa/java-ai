# java-ai

一个面向本地部署场景的 **Java 客服问答系统** 示例项目：
- Java 负责会话编排、Prompt 拼接、调用本地 LLM；
- RAG 服务负责知识库构建与检索；
- Python 提供本地 Embedding API；
- 最终通过 llama-server（OpenAI 兼容接口）完成生成。

---

## 1. 项目架构

```text
用户/前端
   ↓
chat-service (Java, 8081)
  - 会话与消息管理（MySQL）
  - 调用 rag-service 检索知识
  - 拼接 Prompt
  - 调用 llama-server 生成回答
   ↓
rag-service (Java, 8082)
  - 文档上传/解析（Tika）
  - 文本切分
  - 调用 embedding-service 向量化
  - 写入/检索 Qdrant
   ↓
embedding-service (Python, 8090)
  - 加载本地 Embedding 模型
  - 提供 /embeddings API
```

---

## 2. 模块说明

- `services/chat-service`：聊天业务主服务，内置简单 Web UI。
- `services/rag-service`：知识库管理与检索服务。
- `tools/embedding-service`：本地向量模型 API 服务（Python + sentence-transformers）。
- `libs/ai-common`：多服务共享 DTO。
- `legacy-webapp`：历史示例模块（可忽略）。

---

## 3. 环境准备（详细安装步骤）

> 以下步骤默认在 Linux/macOS。Windows 建议使用 WSL2。

## 3.1 安装 JDK 21

1. 安装 OpenJDK 21（任意发行版均可）。
2. 验证：
   ```bash
   java -version
   ```
3. 确保输出包含 `21`。

## 3.2 安装 Maven

1. 安装 Maven 3.9+。
2. 验证：
   ```bash
   mvn -v
   ```

## 3.3 安装 Python 3.10+

1. 验证：
   ```bash
   python3 --version
   ```
2. 建议版本：3.10 / 3.11。

## 3.4 安装 Docker 与 Docker Compose

1. 安装 Docker Desktop 或 Docker Engine。
2. 验证：
   ```bash
   docker --version
   docker compose version
   ```

## 3.5 准备本地 llama-server

你需要自行启动一个支持 OpenAI Chat Completions 协议的 llama-server，并确保可访问：
- 默认地址：`http://localhost:8080`
- 接口路径：`/v1/chat/completions`

> `chat-service` 默认模型名是 `qwen3.5`，可在配置中修改为你实际加载的模型名。

---

## 4. 启动顺序（建议严格按顺序）

## 步骤 1：启动基础设施

在项目根目录执行：
```bash
docker compose up -d
```

会启动：
- MySQL（chat-service 使用）
- Qdrant（rag-service 使用）

## 步骤 2：启动 Python Embedding 服务

```bash
cd tools/embedding-service
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
uvicorn app:app --host 0.0.0.0 --port 8090
```

默认服务地址：`http://localhost:8090`

## 步骤 3：启动 rag-service

在项目根目录新开终端执行：
```bash
mvn -pl services/rag-service spring-boot:run
```

默认地址：`http://localhost:8082`

## 步骤 4：启动 chat-service

在项目根目录再开终端执行：
```bash
mvn -pl services/chat-service spring-boot:run
```

默认地址：`http://localhost:8081`

## 步骤 5：访问页面

浏览器打开：
- `http://localhost:8081`

---

## 5. 核心配置说明

## 5.1 chat-service 配置

文件：`services/chat-service/src/main/resources/application.yml`

- `llm.base-url`：llama-server 地址
- `llm.model`：模型名
- `llm.temperature`：采样温度
- `rag.base-url`：RAG 服务地址
- `chat.history-max-messages`：会话历史窗口大小
- `chat.rag-top-k`：RAG 召回数量

## 5.2 rag-service 配置

文件：`services/rag-service/src/main/resources/application.yml`

- `embedding.base-url`：Embedding 服务地址（Python）
- `embedding.model`：Embedding 模型名
- `rag.qdrant-base-url`：Qdrant 地址
- `rag.collection`：集合名
- `rag.chunk-size`：分块大小
- `rag.chunk-overlap`：分块重叠长度

---

## 6. API 使用示例

## 6.1 上传知识库文档

```bash
curl -X POST http://localhost:8082/api/knowledge/upload \
  -F "files=@/path/to/a.pdf" \
  -F "files=@/path/to/b.docx"
```

## 6.2 检索测试

```bash
curl -X POST http://localhost:8082/api/knowledge/search \
  -H "Content-Type: application/json" \
  -d '{"query":"退款流程是什么？","topK":3}'
```

## 6.3 删除文档

```bash
curl -X POST http://localhost:8082/api/knowledge/delete \
  -H "Content-Type: application/json" \
  -d '{"documentId":"<DOC_ID>"}'
```

## 6.4 聊天接口

- 创建会话：`POST /api/chats`
- 列出会话：`GET /api/chats`
- 获取消息：`GET /api/chats/{chatId}/messages`
- 发送消息：`POST /api/chats/{chatId}/messages`

---

## 7. 设计模式与可扩展性

本次已在 `chat-service` 引入策略模式，提升扩展能力：

1. `PromptBuilder`（策略接口）
   - 默认实现：`RagAwarePromptBuilder`
   - 可扩展：后续可新增“多语言 Prompt”、“行业模板 Prompt”等实现。

2. `HistoryWindowStrategy`（策略接口）
   - 默认实现：`FixedWindowHistoryStrategy`
   - 可扩展：后续可新增“基于 token 截断”策略，避免超长上下文。

这样可以在不改动核心编排流程的前提下，替换 Prompt 与历史裁剪逻辑。

---

## 8. 常见问题

1. **Qdrant 连接失败**
   - 检查 `docker compose up -d` 是否成功。
   - 检查 `rag.qdrant-base-url` 是否正确。

2. **Embedding 下载模型慢/失败**
   - 首次加载模型会下载权重。
   - 可提前手动下载并配置本地缓存。

3. **LLM 无响应**
   - 确认 llama-server 正常启动。
   - 确认 `llm.base-url`、`llm.model` 与实际一致。

---

## 9. 后续建议

- 增加 reranker（重排）提高检索质量；
- 增加知识库版本管理与增量更新；
- 增加回答来源高亮与可追溯性；
- 增加统一观测（日志、trace、指标）。
