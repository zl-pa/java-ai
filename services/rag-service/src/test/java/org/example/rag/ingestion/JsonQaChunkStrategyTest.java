package org.example.rag.ingestion;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.IntStream;
import java.util.List;
import java.util.Map;
import org.example.rag.text.TextChunker;
import org.junit.jupiter.api.Test;

class JsonQaChunkStrategyTest {

  @Test
  void shouldSplitLargeAnswerIntoMultipleChunks() {
    JsonQaChunkStrategy strategy = new JsonQaChunkStrategy(new TextChunker(64, 0));
    String largeAnswer = "A".repeat(500);
    ParsedDocument document =
        new ParsedDocument(
            "json_qa",
            "doc-1",
            "qa",
            "[]",
            Map.of(),
            List.of(new ParsedDocument.QaEntry("What is this?", largeAnswer, Map.of("category", "test"))));

    List<ChunkCandidate> chunks = strategy.chunk(document);

    assertThat(chunks).hasSizeGreaterThan(1);
    assertThat(chunks)
        .allMatch(chunk -> chunk.text().startsWith("Question: What is this?\nAnswer: "));
    assertThat(chunks)
        .allMatch(chunk -> ((Integer) chunk.metadata().get("qaPartTotal")) == chunks.size());
    assertThat(chunks)
        .extracting(ChunkCandidate::chunkIndex)
        .containsExactly(IntStream.range(0, chunks.size()).boxed().toArray(Integer[]::new));
  }
}
