package org.example.chat.rag;

import org.example.aicommon.dto.RagSearchRequest;
import org.example.aicommon.dto.RagSearchResponse;
import org.example.chat.config.RagProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class RagClient {
  private final WebClient webClient;

  public RagClient(RagProperties properties) {
    this.webClient = WebClient.builder().baseUrl(properties.baseUrl()).build();
  }

  public RagSearchResponse search(String query, int topK) {
    RagSearchRequest request = new RagSearchRequest(query, topK);
    RagSearchResponse response =
        webClient
            .post()
            .uri("/api/knowledge/search")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(RagSearchResponse.class)
            .block();
    return response == null ? new RagSearchResponse(java.util.List.of()) : response;
  }
}
