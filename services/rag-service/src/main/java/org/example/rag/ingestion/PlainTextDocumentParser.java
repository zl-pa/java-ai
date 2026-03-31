package org.example.rag.ingestion;

import java.util.List;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(100)
public class PlainTextDocumentParser implements DocumentParser {
  @Override
  public boolean supports(IngestionSource source) {
    return source.content() != null && !source.content().trim().isEmpty();
  }

  @Override
  public ParsedDocument parse(IngestionSource source) {
    return new ParsedDocument(
        "plain_text",
        source.documentId(),
        source.title(),
        source.content(),
        source.metadata(),
        List.of());
  }
}
