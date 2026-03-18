package org.example.chat.prompt;

import java.util.List;
import org.example.aicommon.dto.RagChunk;

/**
 * Prompt 构建策略接口。
 * 采用策略模式，便于后续接入不同提示词模板（客服、销售、法务等）。
 */
public interface PromptBuilder {
  /**
   * 根据检索出的知识片段构建系统提示词。
   *
   * @param chunks RAG 检索结果
   * @return 传递给大模型的系统提示词
   */
  String buildSystemPrompt(List<RagChunk> chunks);
}
