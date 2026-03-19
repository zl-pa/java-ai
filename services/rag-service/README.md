# rag-service

`rag-service` 负责本地知识库构建与检索，是系统的 RAG 基础能力服务。

## 核心职责

1. 文档接入：支持通过上传接口导入文档。
2. 文本解析：使用 Apache Tika 提取文档文本。
3. 文本切分：按 chunk-size/chunk-overlap 切分内容。
4. 向量化：调用 Python embedding-service 获取向量。
5. 向量存储与检索：写入/查询 Qdrant。
6. 文档删除：按 documentId 删除对应所有片段。

## 核心流程（请求链路）

控制器入口：`org.example.rag.api.RagController`

### 1) 上传入库
- `POST /api/knowledge/upload`
- 处理过程：
  1. `RagController` 使用 `org.apache.tika.Tika` 解析文件文本
  2. 组装 `RagDocument`
  3. 调用 `RagOrchestrator#ingest`
  4. 分块、向量化、写入 Qdrant

### 2) 检索
- `POST /api/knowledge/search`
- 处理过程：
  1. 将 query 向量化
  2. 到 Qdrant 搜索 topK
  3. 转换成 `RagChunk` 返回

### 3) 删除
- `POST /api/knowledge/delete`
- 处理过程：
  1. 通过 documentId 删除向量点

## 关键类说明

- `RagOrchestrator`
  - RAG 编排核心，连接“切分、向量化、向量库”流程。

- `text/TextChunker`
  - 文本分块工具，控制 chunk 大小与重叠。

- `embedding/EmbeddingClient`
  - 调用 Python embedding API。

- `qdrant/QdrantClient`
  - 封装集合创建、upsert、search、delete。

- `api/RagController`
  - 对外提供 ingest/upload/search/delete 接口。

## 依赖说明

- 文档解析依赖：
  - `org.apache.tika:tika-parsers-standard-package`
- 向量库：Qdrant（通过 HTTP API 调用）

## 配置文件

路径：`src/main/resources/application.yml`

重点配置：
- `embedding.base-url`：embedding 服务地址
- `embedding.model`：向量模型名
- `rag.qdrant-base-url`：Qdrant 地址
- `rag.collection`：集合名
- `rag.chunk-size` / `rag.chunk-overlap`：切分参数

## 扩展建议

1. 增加批量导入任务队列，避免大文件阻塞请求线程。
2. 增加 metadata 过滤检索（按来源、时间、标签）。
3. 引入 reranker 提升召回后排序质量。
