package org.example.rag.ingestion;

import java.util.Map;

public record ChunkCandidate(int chunkIndex, String text, Map<String, Object> metadata) {}
