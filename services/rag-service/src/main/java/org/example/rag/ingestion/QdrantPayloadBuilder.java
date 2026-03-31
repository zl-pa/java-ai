package org.example.rag.ingestion;

import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class QdrantPayloadBuilder {

  public Map<String, Object> build(
      ParsedDocument document,
      ChunkCandidate chunk,
      String pointId,
      Map<String, Object> documentMetadata) {
    Map<String, Object> payload = new HashMap<>();
    payload.put("pointId", pointId);
    payload.put("documentId", document.documentId());
    payload.put("title", document.title());
    payload.put("chunkIndex", chunk.chunkIndex());
    payload.put("chunkChars", chunk.text().length());
    payload.put("text", chunk.text());
    payload.put("parserType", document.parserType());
    payload.put("metadata", mergeMetadata(documentMetadata, chunk.metadata()));
    return payload;
  }

  private Map<String, Object> mergeMetadata(
      Map<String, Object> documentMetadata, Map<String, Object> chunkMetadata) {
    Map<String, Object> merged = new HashMap<>();
    if (documentMetadata != null) {
      merged.putAll(documentMetadata);
    }
    if (chunkMetadata != null) {
      merged.putAll(chunkMetadata);
    }
    return merged;
  }
}
