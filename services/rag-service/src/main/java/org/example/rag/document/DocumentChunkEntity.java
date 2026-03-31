package org.example.rag.document;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "document_chunk")
public class DocumentChunkEntity {
  @Id private UUID id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "document_id", nullable = false)
  private DocumentEntity document;

  @Column(name = "chunk_index", nullable = false)
  private Integer chunkIndex;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String content;

  @Column(name = "qdrant_point_id", nullable = false)
  private String qdrantPointId;

  @Column(name = "metadata_json")
  private String metadataJson;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @jakarta.persistence.PrePersist
  void prePersist() {
    createdAt = OffsetDateTime.now();
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public DocumentEntity getDocument() {
    return document;
  }

  public void setDocument(DocumentEntity document) {
    this.document = document;
  }

  public Integer getChunkIndex() {
    return chunkIndex;
  }

  public void setChunkIndex(Integer chunkIndex) {
    this.chunkIndex = chunkIndex;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public String getQdrantPointId() {
    return qdrantPointId;
  }

  public void setQdrantPointId(String qdrantPointId) {
    this.qdrantPointId = qdrantPointId;
  }

  public String getMetadataJson() {
    return metadataJson;
  }

  public void setMetadataJson(String metadataJson) {
    this.metadataJson = metadataJson;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }
}
