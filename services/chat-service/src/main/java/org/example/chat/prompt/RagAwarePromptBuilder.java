package org.example.chat.prompt;

import java.util.List;
import org.example.aicommon.dto.RagChunk;
import org.example.chat.config.ChatServiceProperties;
import org.springframework.stereotype.Component;

/**
 * 默认的 RAG 感知提示词构建器。
 *
 * <p>职责：
 * 1. 将检索片段拼接为上下文；
 * 2. 对模型进行行为约束（优先依据上下文回答）；
 * 3. 在上下文缺失时指导模型明确表示“不知道”。
 */
@Component
public class RagAwarePromptBuilder implements PromptBuilder {
  private final ChatServiceProperties properties;

  public RagAwarePromptBuilder(ChatServiceProperties properties) {
    this.properties = properties;
  }

  @Override
  public String buildSystemPrompt(List<RagChunk> chunks) {
    if (chunks == null || chunks.isEmpty()) {
      return "你是一个专业客服助手。请基于事实回答问题；如果缺少依据，请明确回复“我暂时无法从知识库确认该问题”。";
    }

    StringBuilder builder = new StringBuilder();
    builder.append(
        "你是一个专业客服助手。请优先根据【知识库片段】回答，回答简洁、准确、可执行。请直接给出最终答案，不要输出任何推理或思考过程。\n\n");
    builder.append("【知识库片段】\n");

    int index = 1;
    for (RagChunk chunk : chunks) {
      builder.append("[").append(index).append("] ");
      if (chunk.title() != null && !chunk.title().isBlank()) {
        builder.append(chunk.title()).append("：");
      }
      builder.append(truncateChunk(chunk.text())).append("\n");
      index++;
    }

    builder.append("\n【回答要求】\n")
        .append("1. 若知识库能覆盖问题，请直接给出结论和步骤。\n")
        .append("2. 若知识库无法覆盖，请明确说明“暂时无法回答此问题，有需要请联系客服热线”，不要编造信息。\n")
        .append("3. 回答的内容简洁、准确、可执行。\n");

    return builder.toString();
  }

  private String truncateChunk(String text) {
    if (text == null || text.isEmpty()) {
      return "";
    }
    int maxChars = Math.max(properties.ragChunkMaxChars(), 1);
    if (text.length() <= maxChars) {
      return text;
    }
    return text.substring(0, maxChars) + "...(已截断)";
  }
}
