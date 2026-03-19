package org.example.chat.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.example.chat.config.LlmProperties;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

/**
 * 本地 LLM 客户端。
 * 负责调用由 llama-server 暴露的 OpenAI 兼容 Chat Completions 接口。
 */
@Component
public class LlmClient {
  private final WebClient webClient;
  private final LlmProperties properties;
  private final ObjectMapper objectMapper;

  public LlmClient(LlmProperties properties, ObjectMapper objectMapper) {
    this.properties = properties;
    this.objectMapper = objectMapper;
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

  public Flux<String> chatStream(List<LlmMessage> messages) {
    LlmChatRequest request =
        new LlmChatRequest(properties.model(), messages, properties.temperature(), true);

    return webClient
        .post()
        .uri("/v1/chat/completions")
        .accept(MediaType.TEXT_EVENT_STREAM)
        .bodyValue(request)
        .retrieve()
        .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {})
        .map(ServerSentEvent::data)
        .map(String::trim)
        .filter(payload -> !payload.isEmpty())
        .takeUntil("[DONE]"::equals)
        .filter(payload -> !"[DONE]".equals(payload))
        .map(this::extractContent)
        .filter(token -> !token.isEmpty());
  }

  private String extractContent(String payload) {
    try {
      JsonNode root = objectMapper.readTree(payload);
      JsonNode contentNode =
          root.path("choices").path(0).path("delta").path("content");
      return contentNode.isTextual() ? contentNode.asText() : "";
    } catch (Exception ex) {
      return "";
    }
  }
}
