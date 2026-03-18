package org.example.chat.history;

import java.util.List;
import org.example.chat.domain.ChatMessage;

/**
 * 会话历史窗口策略接口。
 * 采用策略模式，便于未来切换为“按 token 截断”等更复杂策略。
 */
public interface HistoryWindowStrategy {

  /**
   * 对历史消息进行裁剪。
   *
   * @param history 原始历史消息（按时间升序）
   * @param maxMessages 最大保留条数
   * @return 裁剪后的历史消息
   */
  List<ChatMessage> trim(List<ChatMessage> history, int maxMessages);
}
