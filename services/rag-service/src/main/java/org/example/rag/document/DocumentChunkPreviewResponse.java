package org.example.rag.document;

import java.util.Map;

public record DocumentChunkPreviewResponse(
    String id,
    int chunkIndex,
    String preview,
    String qdrantPointId,
    Map<String, Object> metadata) {}
