package org.example.rag.text;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class TextChunker {
  private static final Pattern PARAGRAPH_SPLIT = Pattern.compile("\\n\\s*\\n+");
  private static final Pattern SENTENCE_SPLIT = Pattern.compile("(?<=[。！？!?；;.!])");
  private static final int MIN_LOOKBACK = 48;

  private final int chunkSize;
  private final int overlap;

  public TextChunker(int chunkSize, int overlap) {
    this.chunkSize = chunkSize;
    this.overlap = overlap;
  }

  public List<String> chunk(String text) {
    List<String> chunks = new ArrayList<>();
    String normalized = normalize(text);
    if (normalized.isEmpty()) {
      return chunks;
    }

    if (chunkSize <= 0) {
      chunks.add(normalized);
      return chunks;
    }

    String[] paragraphs = PARAGRAPH_SPLIT.split(normalized);
    StringBuilder current = new StringBuilder();

    for (String paragraph : paragraphs) {
      String trimmedParagraph = paragraph.trim();
      if (trimmedParagraph.isEmpty()) {
        continue;
      }

      if (trimmedParagraph.length() > chunkSize) {
        flushCurrent(chunks, current);
        chunks.addAll(splitLargeUnit(trimmedParagraph));
        continue;
      }

      if (current.length() == 0) {
        current.append(trimmedParagraph);
        continue;
      }

      if (current.length() + 2 + trimmedParagraph.length() <= chunkSize) {
        current.append("\n\n").append(trimmedParagraph);
      } else {
        flushCurrent(chunks, current);
        current.append(trimmedParagraph);
      }
    }

    flushCurrent(chunks, current);
    return chunks;
  }

  private List<String> splitLargeUnit(String unit) {
    List<String> chunks = new ArrayList<>();
    String[] sentences = SENTENCE_SPLIT.split(unit);
    StringBuilder current = new StringBuilder();

    for (String sentence : sentences) {
      String trimmedSentence = sentence.trim();
      if (trimmedSentence.isEmpty()) {
        continue;
      }

      if (trimmedSentence.length() > chunkSize) {
        flushCurrent(chunks, current);
        chunks.addAll(splitSlidingWindow(trimmedSentence));
        continue;
      }

      if (current.length() == 0) {
        current.append(trimmedSentence);
        continue;
      }

      if (current.length() + 1 + trimmedSentence.length() <= chunkSize) {
        current.append(' ').append(trimmedSentence);
      } else {
        flushCurrent(chunks, current);
        current.append(trimmedSentence);
      }
    }

    flushCurrent(chunks, current);
    return chunks;
  }

  private List<String> splitSlidingWindow(String text) {
    List<String> chunks = new ArrayList<>();
    int start = 0;
    while (start < text.length()) {
      int targetEnd = Math.min(start + chunkSize, text.length());
      int end = chooseEndBoundary(text, start, targetEnd);
      String chunk = text.substring(start, end).trim();
      if (!chunk.isEmpty()) {
        chunks.add(chunk);
      }
      if (end == text.length()) {
        break;
      }
      if (overlap > 0) {
        start = Math.max(end - overlap, 0);
      } else {
        start = end;
      }
    }
    return chunks;
  }

  private int chooseEndBoundary(String text, int start, int targetEnd) {
    if (targetEnd >= text.length()) {
      return text.length();
    }
    int lowerBound = Math.max(start + 1, targetEnd - Math.max(MIN_LOOKBACK, overlap));
    for (int i = targetEnd; i >= lowerBound; i--) {
      char c = text.charAt(i - 1);
      if (isBoundary(c)) {
        return i;
      }
    }
    return targetEnd;
  }

  private boolean isBoundary(char c) {
    return Character.isWhitespace(c) || "，,。！？!?；;：:".indexOf(c) >= 0;
  }

  private void flushCurrent(List<String> chunks, StringBuilder current) {
    String chunk = current.toString().trim();
    if (!chunk.isEmpty()) {
      chunks.add(chunk);
    }
    current.setLength(0);
  }

  private String normalize(String text) {
    return text == null ? "" : text.replace("\r\n", "\n").trim();
  }
}
