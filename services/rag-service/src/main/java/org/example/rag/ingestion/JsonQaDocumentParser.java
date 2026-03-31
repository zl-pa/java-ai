package org.example.rag.ingestion;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(10)
public class JsonQaDocumentParser implements DocumentParser {
  private final ObjectMapper objectMapper;

  public JsonQaDocumentParser(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public boolean supports(IngestionSource source) {
    String content = source.content();
    if (content == null || content.trim().isEmpty() || !content.trim().startsWith("[")) {
      return false;
    }
    try {
      JsonNode node = objectMapper.readTree(content);
      if (!node.isArray() || node.isEmpty()) {
        return false;
      }
      for (JsonNode item : node) {
        if (!item.isObject()
            || item.path("question").asText("").isBlank()
            || item.path("answer").asText("").isBlank()) {
          return false;
        }
      }
      return true;
    } catch (Exception ex) {
      return false;
    }
  }

  @Override
  public ParsedDocument parse(IngestionSource source) {
    try {
      JsonNode root = objectMapper.readTree(source.content());
      List<ParsedDocument.QaEntry> entries = new ArrayList<>();
      for (JsonNode node : root) {
        String question = node.path("question").asText();
        String answer = node.path("answer").asText();
        Map<String, Object> metadata = new HashMap<>();
        if (node.has("category")) {
          metadata.put("category", node.get("category").asText());
        }
        if (node.has("title")) {
          metadata.put("title", node.get("title").asText());
        }
        if (node.has("tag")) {
          metadata.put("tag", node.get("tag").asText());
        }
        if (node.has("tags")) {
          metadata.put("tags", objectMapper.convertValue(node.get("tags"), new TypeReference<List<String>>() {}));
        }
        entries.add(new ParsedDocument.QaEntry(question, answer, metadata));
      }
      return new ParsedDocument(
          "json_qa",
          source.documentId(),
          source.title(),
          source.content(),
          source.metadata(),
          entries);
    } catch (Exception ex) {
      throw new IllegalArgumentException("Invalid JSON Q&A document", ex);
    }
  }
}
