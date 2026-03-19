package org.example.rag.qdrant;

import java.util.List;
import java.util.Map;
import org.example.rag.config.RagProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
public class QdrantClient {
  private final WebClient webClient;
  private final RagProperties properties;

  public QdrantClient(RagProperties properties) {
    this.properties = properties;
    this.webClient = WebClient.builder().baseUrl(properties.qdrantBaseUrl()).build();
  }

  public void ensureCollection(int vectorSize) {
    try {
      webClient.get().uri("/collections/{name}", properties.collection()).retrieve().toBodilessEntity().block();
      return;
    } catch (WebClientResponseException ex) {
      if (ex.getStatusCode() != HttpStatus.NOT_FOUND) {
        throw ex;
      }
    }

    CreateCollectionRequest request =
        new CreateCollectionRequest(new VectorConfig(vectorSize, "Cosine"));
    webClient
        .put()
        .uri("/collections/{name}", properties.collection())
        .bodyValue(request)
        .retrieve()
        .toBodilessEntity()
        .block();
  }

  public void upsert(List<QdrantPoint> points) {
    UpsertRequest request = new UpsertRequest(points);
      try {
          webClient
                  .put()
                  .uri("/collections/{name}/points", properties.collection())
                  .bodyValue(request)
                  .retrieve()
                  .toBodilessEntity()
                  .block();
      } catch (WebClientResponseException e) {
          // 【关键】这里会打印出 Qdrant 到底报了什么错
          System.err.println("==========================================");
          System.err.println("❌ Qdrant 400 Bad Request 详细信息:");
          System.err.println("响应内容 (Response Body): " + e.getResponseBodyAsString());
          System.err.println("==========================================");

          // 抛出异常中断程序，让你能看到上面的日志
          throw new RuntimeException("Qdrant 插入失败: " + e.getResponseBodyAsString(), e);
      }
  }

  public List<ScoredPoint> search(List<Double> vector, int limit) {
    SearchRequest request = new SearchRequest(vector, limit, true);
    SearchResponse response =
        webClient
            .post()
            .uri("/collections/{name}/points/search", properties.collection())
            .bodyValue(request)
            .retrieve()
            .bodyToMono(SearchResponse.class)
            .block();
    if (response == null || response.result() == null) {
      return List.of();
    }
    return response.result();
  }

  public void deleteByDocId(String docId) {
    DeleteRequest request =
        new DeleteRequest(
            new DeleteFilter(
                List.of(new DeleteCondition("doc_id", new DeleteMatch(docId)))));
    webClient
        .post()
        .uri("/collections/{name}/points/delete", properties.collection())
        .bodyValue(request)
        .retrieve()
        .toBodilessEntity()
        .block();
  }

  public record VectorConfig(int size, String distance) {}

  public record CreateCollectionRequest(VectorConfig vectors) {}

  public record QdrantPoint(String id, List<Double> vector, Map<String, Object> payload) {}

  public record UpsertRequest(List<QdrantPoint> points) {}

  public record SearchRequest(List<Double> vector, int limit, boolean with_payload) {}

  public record SearchResponse(List<ScoredPoint> result) {}

  public record ScoredPoint(String id, double score, Map<String, Object> payload) {}

  public record DeleteRequest(DeleteFilter filter) {}

  public record DeleteFilter(List<DeleteCondition> must) {}

  public record DeleteCondition(String key, DeleteMatch match) {}

  public record DeleteMatch(String value) {}
}
