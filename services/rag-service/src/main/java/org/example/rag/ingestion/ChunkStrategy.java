package org.example.rag.ingestion;

import java.util.List;

public interface ChunkStrategy {
  boolean supports(ParsedDocument document);

  List<ChunkCandidate> chunk(ParsedDocument document);
}
