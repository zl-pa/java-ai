# chat-service

`chat-service` 是系统的业务编排中心，负责“智能客服问答”主流程。

## 核心职责

1. 会话管理：创建会话、查询会话列表、查询历史消息。
2. 问答编排：接收用户问题，调用 RAG 检索，拼接 Prompt，再调用本地 LLM。
3. 结果返回：返回模型回答与命中的知识片段，供前端展示。

## 核心流程（请求链路）

入口控制器：`org.example.chat.api.ChatController`

当用户发送消息时，主要链路如下：

1. `ChatController#postMessage`
2. `ChatOrchestrator#postMessage`
   - 保存用户消息
   - 读取会话历史并通过 `HistoryWindowStrategy` 裁剪
   - 调用 `RagClient` 到 `rag-service` 检索知识片段
   - 通过 `PromptBuilder` 构建系统提示词
   - 调用 `LlmClient` 请求 llama-server
   - 保存助手回复并返回

## 关键类说明

- `ChatOrchestrator`
  - 领域编排核心，聚合所有子能力。

- `prompt/PromptBuilder` + `prompt/RagAwarePromptBuilder`
  - 提示词策略扩展点（策略模式）。
  - 可新增行业模板、语言模板、角色模板。

- `history/HistoryWindowStrategy` + `history/FixedWindowHistoryStrategy`
  - 历史裁剪策略扩展点（策略模式）。
  - 当前实现：固定保留最近 N 条消息。

- `rag/RagClient`
  - 与 `rag-service` 交互，获取检索结果。

- `llm/LlmClient`
  - 与 llama-server 的 OpenAI 兼容接口交互。

## 配置文件

路径：`src/main/resources/application.yml`

重点配置：
- `llm.base-url`：本地 LLM 地址
- `llm.model`：模型名
- `rag.base-url`：RAG 服务地址
- `chat.history-max-messages`：历史窗口大小
- `chat.rag-top-k`：RAG 召回数

## 扩展建议

1. 新增 `TokenWindowHistoryStrategy` 实现按 token 截断。
2. 新增 `CitationPromptBuilder` 强制输出引用片段编号。
3. 对 `LlmClient` 增加超时、重试、熔断与降级策略。
