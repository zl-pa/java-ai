package org.example.chat.history;

import java.util.List;
import org.example.chat.domain.ChatMessage;
import org.springframework.stereotype.Component;

/**
 * 固定窗口历史策略：仅保留最近 N 条消息。
 */
@Component
public class FixedWindowHistoryStrategy implements HistoryWindowStrategy {

  @Override
  public List<ChatMessage> trim(List<ChatMessage> history, int maxMessages) {
    if (history == null || history.isEmpty()) {
      return List.of();
    }
    if (maxMessages <= 0) {
      return List.of();
    }
    if (history.size() <= maxMessages) {
      return history;
    }
    return history.subList(Math.max(history.size() - maxMessages, 0), history.size());
  }
}
