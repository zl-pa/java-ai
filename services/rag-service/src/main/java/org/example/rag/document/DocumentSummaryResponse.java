package org.example.rag.document;

import java.time.OffsetDateTime;

public record DocumentSummaryResponse(
    String id,
    String title,
    String parserType,
    long chunkCount,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {}
