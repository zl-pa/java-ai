package org.example.rag.document;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunkEntity, UUID> {
  List<DocumentChunkEntity> findByDocument_IdOrderByChunkIndexAsc(UUID documentId);

  long countByDocument_Id(UUID documentId);

  void deleteByDocument_Id(UUID documentId);
}
