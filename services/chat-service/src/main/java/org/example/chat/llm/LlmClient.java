package org.example.chat.llm;

import java.util.List;
import org.example.chat.config.LlmProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * 本地 LLM 客户端。
 * 负责调用由 llama-server 暴露的 OpenAI 兼容 Chat Completions 接口。
 */
@Component
public class LlmClient {
  private final WebClient webClient;
  private final LlmProperties properties;

  public LlmClient(LlmProperties properties) {
    this.properties = properties;
    this.webClient = WebClient.builder().baseUrl(properties.baseUrl()).build();
  }

  public String chat(List<LlmMessage> messages) {
    // LLM 的 base-url 与 model 通过配置文件统一管理，便于环境切换。
    LlmChatRequest request =
        new LlmChatRequest(properties.model(), messages, properties.temperature(), false);
    LlmChatResponse response =
        webClient
            .post()
            .uri("/v1/chat/completions")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(LlmChatResponse.class)
            .block();
    if (response == null || response.choices() == null || response.choices().isEmpty()) {
      return "";
    }
    LlmMessage message = response.choices().get(0).message();
    return message == null ? "" : message.content();
  }
}
