package org.example.rag.ingestion;

import java.util.ArrayList;
import java.util.List;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(10)
public class JsonQaChunkStrategy implements ChunkStrategy {

  @Override
  public boolean supports(ParsedDocument document) {
    return "json_qa".equals(document.parserType());
  }

  @Override
  public List<ChunkCandidate> chunk(ParsedDocument document) {
    List<ChunkCandidate> chunks = new ArrayList<>();
    for (int i = 0; i < document.qaEntries().size(); i++) {
      ParsedDocument.QaEntry entry = document.qaEntries().get(i);
      String text = "Question: " + entry.question() + "\nAnswer: " + entry.answer();
      chunks.add(new ChunkCandidate(i, text, entry.metadata()));
    }
    return chunks;
  }
}
