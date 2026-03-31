package org.example.rag.api;

import jakarta.validation.Valid;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.tika.Tika;
import org.example.aicommon.dto.RagDeleteRequest;
import org.example.aicommon.dto.RagDocument;
import org.example.aicommon.dto.RagIngestRequest;
import org.example.aicommon.dto.RagSearchRequest;
import org.example.aicommon.dto.RagSearchResponse;
import org.example.rag.RagOrchestrator;
import org.example.rag.document.DocumentApplicationService;
import org.example.rag.document.DocumentDetailResponse;
import org.example.rag.document.DocumentStatsResponse;
import org.example.rag.document.DocumentSummaryResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping
public class RagController {
  private final RagOrchestrator orchestrator;
  private final DocumentApplicationService documentApplicationService;
  private final Tika tika = new Tika();

  public RagController(
      RagOrchestrator orchestrator, DocumentApplicationService documentApplicationService) {
    this.orchestrator = orchestrator;
    this.documentApplicationService = documentApplicationService;
  }

  @PostMapping("/api/knowledge/ingest")
  public ResponseEntity<Void> ingest(@Valid @RequestBody RagIngestRequest request) {
    orchestrator.ingest(request);
    return ResponseEntity.accepted().build();
  }

  @PostMapping("/api/knowledge/upload")
  public ResponseEntity<List<String>> upload(@RequestPart("files") List<MultipartFile> files)
      throws IOException {

    List<RagDocument> documents = new ArrayList<>();
    for (MultipartFile file : files) {
      if (file.isEmpty()) {
        continue;
      }

      String originalFilename = file.getOriginalFilename();
      String content = extractContent(file, originalFilename);
      String docId = UUID.randomUUID().toString();
      documents.add(new RagDocument(docId, originalFilename, content, null));
    }

    if (documents.isEmpty()) {
      return ResponseEntity.badRequest().build();
    }

    orchestrator.ingest(new RagIngestRequest(documents));
    List<String> ids = documents.stream().map(RagDocument::id).toList();
    return ResponseEntity.accepted().body(ids);
  }

  @PostMapping("/api/knowledge/search")
  public RagSearchResponse search(@Valid @RequestBody RagSearchRequest request) {
    return orchestrator.search(request);
  }

  @PostMapping("/api/knowledge/delete")
  public ResponseEntity<Void> delete(@Valid @RequestBody RagDeleteRequest request) {
    orchestrator.deleteByDocumentId(request.documentId());
    return ResponseEntity.ok().build();
  }

  @GetMapping("/api/rag/documents")
  public List<DocumentSummaryResponse> listDocuments() {
    return documentApplicationService.listDocuments();
  }

  @GetMapping("/api/rag/documents/{id}")
  public DocumentDetailResponse getDocument(@PathVariable("id") UUID id) {
    return documentApplicationService.getDocument(id);
  }

  @DeleteMapping("/api/rag/documents/{id}")
  public ResponseEntity<Void> deleteDocument(@PathVariable("id") UUID id) {
    orchestrator.deleteByDocumentId(id.toString());
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/api/rag/documents/{id}/reindex")
  public ResponseEntity<Void> reindexDocument(@PathVariable("id") UUID id) {
    orchestrator.reindexDocument(id.toString());
    return ResponseEntity.accepted().build();
  }

  @GetMapping("/api/rag/stats")
  public DocumentStatsResponse stats() {
    return documentApplicationService.stats();
  }

  private String extractContent(MultipartFile file, String originalFilename) throws IOException {
    if (originalFilename != null
        && (originalFilename.endsWith(".txt")
            || originalFilename.endsWith(".md")
            || originalFilename.endsWith(".csv")
            || originalFilename.endsWith(".json")
            || "text/plain".equals(file.getContentType())
            || "application/json".equals(file.getContentType()))) {
      return new String(file.getBytes(), StandardCharsets.UTF_8);
    }

    try (InputStream inputStream = file.getInputStream()) {
      return tika.parseToString(inputStream);
    } catch (Exception e) {
      throw new RuntimeException("解析文件失败: " + originalFilename, e);
    }
  }
}
