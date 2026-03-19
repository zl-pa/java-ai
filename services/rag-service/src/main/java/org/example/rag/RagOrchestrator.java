package org.example.rag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.example.aicommon.dto.RagChunk;
import org.example.aicommon.dto.RagDocument;
import org.example.aicommon.dto.RagIngestRequest;
import org.example.aicommon.dto.RagSearchRequest;
import org.example.aicommon.dto.RagSearchResponse;
import org.example.rag.config.RagProperties;
import org.example.rag.embedding.EmbeddingClient;
import org.example.rag.qdrant.QdrantClient;
import org.example.rag.text.TextChunker;
import org.springframework.stereotype.Service;

@Service
public class RagOrchestrator {
  private final EmbeddingClient embeddingClient;
  private final QdrantClient qdrantClient;
  private final TextChunker chunker;

  public RagOrchestrator(
      EmbeddingClient embeddingClient, QdrantClient qdrantClient, RagProperties properties) {
    this.embeddingClient = embeddingClient;
    this.qdrantClient = qdrantClient;
    this.chunker = new TextChunker(properties.chunkSize(), properties.chunkOverlap());
  }

  // Build embeddings for each chunk and store them in the vector DB.
  public void ingest(RagIngestRequest request) {
    List<QdrantClient.QdrantPoint> points = new ArrayList<>();
    Integer embeddingSize = null;
    boolean collectionEnsured = false;

    for (RagDocument doc : request.documents()) {
      List<String> chunks = chunker.chunk(doc.content());
      for (int i = 0; i < chunks.size(); i++) {
        String chunkText = chunks.get(i);
        List<Double> embedding = embeddingClient.embed(chunkText);
        if (embedding.isEmpty()) {
          continue;
        }

        if (embeddingSize == null) {
          embeddingSize = embedding.size();
        } else if (embeddingSize != embedding.size()) {
          throw new IllegalStateException(
              "Embedding dimension changed within the same ingest request: expected="
                  + embeddingSize
                  + ", actual="
                  + embedding.size()
                  + ", docId="
                  + doc.id()
                  + ", chunkIndex="
                  + i);
        }

        if (!collectionEnsured) {
          qdrantClient.ensureCollection(embeddingSize);
          collectionEnsured = true;
        }

        String pointId = java.util.UUID.randomUUID().toString();
        Map<String, Object> payload = new HashMap<>();
        payload.put("doc_id", doc.id());
        payload.put("title", doc.title());
        payload.put("chunk_index", i);
        payload.put("chunk_chars", chunkText.length());
        payload.put("text", chunkText);
        if (doc.metadata() != null) {
          payload.put("metadata", doc.metadata());
        }
        points.add(new QdrantClient.QdrantPoint(pointId, embedding, payload));
      }
    }
    if (!points.isEmpty()) {
      qdrantClient.upsert(points);
    }
  }

  public RagSearchResponse search(RagSearchRequest request) {
    List<Double> embedding = embeddingClient.embed(request.query());
    if (embedding.isEmpty()) {
      return new RagSearchResponse(List.of());
    }
    qdrantClient.ensureCollection(embedding.size());
    List<QdrantClient.ScoredPoint> results = qdrantClient.search(embedding, request.topK());
    List<RagChunk> chunks = new ArrayList<>();
    for (QdrantClient.ScoredPoint point : results) {
      Map<String, Object> payload = point.payload();
      String text = payload == null ? "" : String.valueOf(payload.getOrDefault("text", ""));
      String title = payload == null ? null : (String) payload.get("title");
      Map<String, Object> metadata =
          payload == null ? Map.of() : (Map<String, Object>) payload.getOrDefault("metadata", Map.of());
      chunks.add(new RagChunk(point.id(), text, title, point.score(), metadata));
    }
    return new RagSearchResponse(chunks);
  }

  // Remove all chunks that belong to a document ID.
  public void deleteByDocumentId(String documentId) {
    qdrantClient.deleteByDocId(documentId);
  }
}
