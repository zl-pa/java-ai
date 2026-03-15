package org.example.rag.text;

import java.util.ArrayList;
import java.util.List;

public class TextChunker {
  private final int chunkSize;
  private final int overlap;

  public TextChunker(int chunkSize, int overlap) {
    this.chunkSize = chunkSize;
    this.overlap = overlap;
  }

  public List<String> chunk(String text) {
    List<String> chunks = new ArrayList<>();
    String normalized = text.replace("\r\n", "\n").trim();
    if (normalized.isEmpty()) {
      return chunks;
    }
    int start = 0;
    while (start < normalized.length()) {
      int end = Math.min(start + chunkSize, normalized.length());
      String chunk = normalized.substring(start, end).trim();
      if (!chunk.isEmpty()) {
        chunks.add(chunk);
      }
      if (end == normalized.length()) {
        break;
      }
      start = Math.max(end - overlap, 0);
    }
    return chunks;
  }
}
