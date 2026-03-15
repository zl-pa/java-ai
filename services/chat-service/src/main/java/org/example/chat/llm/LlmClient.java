package org.example.chat.llm;

import java.util.List;
import org.example.chat.config.LlmProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class LlmClient {
  private final WebClient webClient;
  private final LlmProperties properties;

  public LlmClient(LlmProperties properties) {
    this.properties = properties;
    this.webClient = WebClient.builder().baseUrl(properties.baseUrl()).build();
  }

  public String chat(List<LlmMessage> messages) {
    // LLM API base URL and model are configured in application.yml.
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
