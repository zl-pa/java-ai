package org.example.rag.ingestion;

public interface DocumentParser {
  boolean supports(IngestionSource source);

  ParsedDocument parse(IngestionSource source);
}
