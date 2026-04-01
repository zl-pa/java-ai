package org.example.rag.ingestion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.example.rag.text.TextChunker;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(10)
public class JsonQaChunkStrategy implements ChunkStrategy {
  private final TextChunker chunker;

  public JsonQaChunkStrategy(TextChunker chunker) {
    this.chunker = chunker;
  }

  @Override
  public boolean supports(ParsedDocument document) {
    return "json_qa".equals(document.parserType());
  }

  @Override
  public List<ChunkCandidate> chunk(ParsedDocument document) {
    List<ChunkCandidate> chunks = new ArrayList<>();
    int chunkIndex = 0;
    for (ParsedDocument.QaEntry entry : document.qaEntries()) {
      List<String> answerParts = splitAnswer(entry.answer());
      for (int i = 0; i < answerParts.size(); i++) {
        Map<String, Object> metadata = new HashMap<>();
        if (entry.metadata() != null) {
          metadata.putAll(entry.metadata());
        }
        if (answerParts.size() > 1) {
          metadata.put("qaPart", i + 1);
          metadata.put("qaPartTotal", answerParts.size());
        }
        String text = "Question: " + entry.question() + "\nAnswer: " + answerParts.get(i);
        chunks.add(new ChunkCandidate(chunkIndex++, text, metadata));
      }
    }
    return chunks;
  }

  private List<String> splitAnswer(String answer) {
    List<String> answerParts = chunker.chunk(answer == null ? "" : answer);
    if (answerParts.isEmpty()) {
      return List.of("");
    }
    return answerParts;
  }
}
