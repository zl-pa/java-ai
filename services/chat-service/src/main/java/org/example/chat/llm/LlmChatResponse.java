package org.example.chat.llm;

import java.util.List;

public record LlmChatResponse(List<Choice> choices) {
  public record Choice(LlmMessage message) {}
}
