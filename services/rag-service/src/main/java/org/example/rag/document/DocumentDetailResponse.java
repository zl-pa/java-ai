package org.example.rag.document;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record DocumentDetailResponse(
    String id,
    String title,
    String parserType,
    Map<String, Object> metadata,
    long chunkCount,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    List<DocumentChunkPreviewResponse> chunks) {}
