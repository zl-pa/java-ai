package org.example.rag.ingestion;

import java.util.List;
import java.util.Map;

public record ParsedDocument(
    String parserType,
    String documentId,
    String title,
    String originalContent,
    Map<String, Object> metadata,
    List<QaEntry> qaEntries) {

  public record QaEntry(String question, String answer, Map<String, Object> metadata) {}
}
