package org.example.rag.ingestion;

import java.util.List;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Component;

@Component
public class IngestionPipeline {
  private final List<DocumentParser> parsers;
  private final List<ChunkStrategy> strategies;

  public IngestionPipeline(List<DocumentParser> parsers, List<ChunkStrategy> strategies) {
    this.parsers = parsers.stream().sorted(AnnotationAwareOrderComparator.INSTANCE).toList();
    this.strategies = strategies.stream().sorted(AnnotationAwareOrderComparator.INSTANCE).toList();
  }

  public ParsedDocument parse(IngestionSource source) {
    return parsers.stream()
        .filter(parser -> parser.supports(source))
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("No parser supports source: " + source.title()))
        .parse(source);
  }

  public List<ChunkCandidate> chunk(ParsedDocument document) {
    return strategies.stream()
        .filter(strategy -> strategy.supports(document))
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "No chunk strategy supports parserType=" + document.parserType()))
        .chunk(document);
  }
}
