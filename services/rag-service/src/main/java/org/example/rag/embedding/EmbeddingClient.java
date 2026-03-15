package org.example.rag.embedding;

import java.util.List;
import org.example.rag.config.EmbeddingProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class EmbeddingClient {
  private final WebClient webClient;
  private final EmbeddingProperties properties;

  public EmbeddingClient(EmbeddingProperties properties) {
    this.properties = properties;
    this.webClient = WebClient.builder().baseUrl(properties.baseUrl()).build();
  }

  public List<Double> embed(String text) {
    // Embedding API is a local service you control (not Ollama).
    EmbeddingRequest request = new EmbeddingRequest(properties.model(), text);
    EmbeddingResponse response =
        webClient
            .post()
            .uri("/embeddings")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(EmbeddingResponse.class)
            .block();
    if (response == null || response.embedding() == null) {
      return List.of();
    }
    return response.embedding();
  }
}
