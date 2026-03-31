package org.example.rag.document;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DocumentApplicationService {
  private static final int PREVIEW_MAX = 200;

  private final DocumentRepository documentRepository;
  private final DocumentChunkRepository chunkRepository;
  private final ObjectMapper objectMapper;

  public DocumentApplicationService(
      DocumentRepository documentRepository,
      DocumentChunkRepository chunkRepository,
      ObjectMapper objectMapper) {
    this.documentRepository = documentRepository;
    this.chunkRepository = chunkRepository;
    this.objectMapper = objectMapper;
  }

  @Transactional(readOnly = true)
  public List<DocumentSummaryResponse> listDocuments() {
    return documentRepository.findAll().stream()
        .map(
            entity ->
                new DocumentSummaryResponse(
                    entity.getId().toString(),
                    entity.getTitle(),
                    entity.getParserType(),
                    chunkRepository.countByDocument_Id(entity.getId()),
                    entity.getCreatedAt(),
                    entity.getUpdatedAt()))
        .toList();
  }

  @Transactional(readOnly = true)
  public DocumentDetailResponse getDocument(UUID id) {
    DocumentEntity entity =
        documentRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Document not found: " + id));
    List<DocumentChunkPreviewResponse> chunks =
        chunkRepository.findByDocument_IdOrderByChunkIndexAsc(id).stream()
            .map(
                chunk ->
                    new DocumentChunkPreviewResponse(
                        chunk.getId().toString(),
                        chunk.getChunkIndex(),
                        toPreview(chunk.getContent()),
                        chunk.getQdrantPointId(),
                        fromJson(chunk.getMetadataJson())))
            .toList();
    return new DocumentDetailResponse(
        entity.getId().toString(),
        entity.getTitle(),
        entity.getParserType(),
        fromJson(entity.getMetadataJson()),
        chunks.size(),
        entity.getCreatedAt(),
        entity.getUpdatedAt(),
        chunks);
  }

  @Transactional(readOnly = true)
  public DocumentStatsResponse stats() {
    return new DocumentStatsResponse(documentRepository.count(), chunkRepository.count());
  }

  @Transactional
  public void deleteDocumentRecord(UUID id) {
    chunkRepository.deleteByDocument_Id(id);
    documentRepository.deleteById(id);
  }

  public String toJson(Map<String, Object> metadata) {
    if (metadata == null || metadata.isEmpty()) {
      return null;
    }
    try {
      return objectMapper.writeValueAsString(metadata);
    } catch (Exception ex) {
      throw new IllegalStateException("Unable to serialize metadata", ex);
    }
  }

  public Map<String, Object> fromJson(String json) {
    if (json == null || json.isBlank()) {
      return Map.of();
    }
    try {
      return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
    } catch (Exception ex) {
      throw new IllegalStateException("Unable to deserialize metadata", ex);
    }
  }

  private String toPreview(String content) {
    if (content == null) {
      return "";
    }
    String normalized = content.replace("\n", " ").trim();
    if (normalized.length() <= PREVIEW_MAX) {
      return normalized;
    }
    return normalized.substring(0, PREVIEW_MAX) + "...";
  }
}
