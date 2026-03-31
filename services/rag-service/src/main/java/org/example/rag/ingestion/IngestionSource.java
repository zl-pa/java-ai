package org.example.rag.ingestion;

import java.util.Map;

public record IngestionSource(
    String documentId, String title, String content, Map<String, Object> metadata) {}
