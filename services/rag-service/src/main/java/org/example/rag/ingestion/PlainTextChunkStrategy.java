package org.example.rag.ingestion;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.example.rag.text.TextChunker;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(100)
public class PlainTextChunkStrategy implements ChunkStrategy {
  private final TextChunker chunker;

  public PlainTextChunkStrategy(TextChunker chunker) {
    this.chunker = chunker;
  }

  @Override
  public boolean supports(ParsedDocument document) {
    return "plain_text".equals(document.parserType());
  }

  @Override
  public List<ChunkCandidate> chunk(ParsedDocument document) {
    List<String> chunks = chunker.chunk(document.originalContent());
    List<ChunkCandidate> candidates = new ArrayList<>();
    for (int i = 0; i < chunks.size(); i++) {
      candidates.add(new ChunkCandidate(i, chunks.get(i), Map.of()));
    }
    return candidates;
  }
}
