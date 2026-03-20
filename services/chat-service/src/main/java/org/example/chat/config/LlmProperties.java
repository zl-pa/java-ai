package org.example.chat.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "llm")
public record LlmProperties(
    String baseUrl, String chatPath, String apiKey, String model, Double temperature) {
  public String resolvedChatPath() {
    return (chatPath == null || chatPath.isBlank()) ? "/v1/chat/completions" : chatPath;
  }
}
