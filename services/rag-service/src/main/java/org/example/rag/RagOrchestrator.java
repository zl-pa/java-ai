package org.example.rag;

import jakarta.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.example.aicommon.dto.RagChunk;
import org.example.aicommon.dto.RagDocument;
import org.example.aicommon.dto.RagIngestRequest;
import org.example.aicommon.dto.RagSearchRequest;
import org.example.aicommon.dto.RagSearchResponse;
import org.example.rag.document.DocumentApplicationService;
import org.example.rag.document.DocumentChunkEntity;
import org.example.rag.document.DocumentChunkRepository;
import org.example.rag.document.DocumentEntity;
import org.example.rag.document.DocumentRepository;
import org.example.rag.embedding.EmbeddingClient;
import org.example.rag.ingestion.ChunkCandidate;
import org.example.rag.ingestion.IngestionPipeline;
import org.example.rag.ingestion.IngestionSource;
import org.example.rag.ingestion.ParsedDocument;
import org.example.rag.ingestion.QdrantPayloadBuilder;
import org.example.rag.qdrant.QdrantClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RagOrchestrator {
  private final EmbeddingClient embeddingClient;
  private final QdrantClient qdrantClient;
  private final IngestionPipeline ingestionPipeline;
  private final QdrantPayloadBuilder payloadBuilder;
  private final DocumentRepository documentRepository;
  private final DocumentChunkRepository chunkRepository;
  private final DocumentApplicationService documentApplicationService;

  public RagOrchestrator(
      EmbeddingClient embeddingClient,
      QdrantClient qdrantClient,
      IngestionPipeline ingestionPipeline,
      QdrantPayloadBuilder payloadBuilder,
      DocumentRepository documentRepository,
      DocumentChunkRepository chunkRepository,
      DocumentApplicationService documentApplicationService) {
    this.embeddingClient = embeddingClient;
    this.qdrantClient = qdrantClient;
    this.ingestionPipeline = ingestionPipeline;
    this.payloadBuilder = payloadBuilder;
    this.documentRepository = documentRepository;
    this.chunkRepository = chunkRepository;
    this.documentApplicationService = documentApplicationService;
  }

  @Transactional
  public void ingest(RagIngestRequest request) {
    for (RagDocument doc : request.documents()) {
      ingestSingle(doc, true);
    }
  }

  @Transactional
  public void reindexDocument(String documentId) {
    UUID id = UUID.fromString(documentId);
    DocumentEntity existing =
        documentRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Document not found: " + id));
    RagDocument doc =
        new RagDocument(
            existing.getId().toString(),
            existing.getTitle(),
            existing.getContent(),
            documentApplicationService.fromJson(existing.getMetadataJson()));
    ingestSingle(doc, true);
  }

  private void ingestSingle(RagDocument doc, boolean cleanExisting) {
    String docId = doc.id();
    if (cleanExisting) {
      qdrantClient.deleteByDocumentId(docId);
      UUID uuid = UUID.fromString(docId);
      chunkRepository.deleteByDocument_Id(uuid);
    }

    ParsedDocument parsed =
        ingestionPipeline.parse(new IngestionSource(doc.id(), doc.title(), doc.content(), doc.metadata()));
    List<ChunkCandidate> chunks = ingestionPipeline.chunk(parsed);

    List<QdrantClient.QdrantPoint> points = new ArrayList<>();
    List<DocumentChunkEntity> chunkEntities = new ArrayList<>();
    Integer embeddingSize = null;
    boolean collectionEnsured = false;

    for (ChunkCandidate chunk : chunks) {
      List<Double> embedding = embeddingClient.embed(chunk.text());
      if (embedding.isEmpty()) {
        continue;
      }
      if (embeddingSize == null) {
        embeddingSize = embedding.size();
      } else if (embeddingSize != embedding.size()) {
        throw new IllegalStateException("Embedding dimension changed within request");
      }

      if (!collectionEnsured) {
        qdrantClient.ensureCollection(embeddingSize);
        collectionEnsured = true;
      }

      String pointId = UUID.randomUUID().toString();
      Map<String, Object> payload = payloadBuilder.build(parsed, chunk, pointId, doc.metadata());
      points.add(new QdrantClient.QdrantPoint(pointId, embedding, payload));

      DocumentChunkEntity chunkEntity = new DocumentChunkEntity();
      chunkEntity.setId(UUID.randomUUID());
      chunkEntity.setChunkIndex(chunk.chunkIndex());
      chunkEntity.setContent(chunk.text());
      chunkEntity.setQdrantPointId(pointId);
      chunkEntity.setMetadataJson(documentApplicationService.toJson(chunk.metadata()));
      chunkEntities.add(chunkEntity);
    }

    if (!points.isEmpty()) {
      qdrantClient.upsert(points);
    }

    DocumentEntity entity = documentRepository.findById(UUID.fromString(docId)).orElseGet(DocumentEntity::new);
    entity.setId(UUID.fromString(docId));
    entity.setTitle(doc.title() == null ? docId : doc.title());
    entity.setParserType(parsed.parserType());
    entity.setContent(doc.content());
    entity.setMetadataJson(documentApplicationService.toJson(doc.metadata()));
    DocumentEntity saved = documentRepository.save(entity);

    chunkEntities.forEach(chunkEntity -> chunkEntity.setDocument(saved));
    chunkRepository.saveAll(chunkEntities);
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

  @Transactional
  public void deleteByDocumentId(String documentId) {
    qdrantClient.deleteByDocumentId(documentId);
    documentApplicationService.deleteDocumentRecord(UUID.fromString(documentId));
  }
}
