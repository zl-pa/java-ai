package org.example.rag.api;

import jakarta.validation.Valid;
import java.io.IOException;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/knowledge")
public class RagController {
  private final RagOrchestrator orchestrator;
  private final Tika tika = new Tika();

  public RagController(RagOrchestrator orchestrator) {
    this.orchestrator = orchestrator;
  }

  @PostMapping("/ingest")
  public ResponseEntity<Void> ingest(@Valid @RequestBody RagIngestRequest request) {
    orchestrator.ingest(request);
    return ResponseEntity.accepted().build();
  }

  @PostMapping("/upload")
  public ResponseEntity<List<String>> upload(@RequestPart("files") List<MultipartFile> files)
      throws IOException {
    List<RagDocument> documents = new ArrayList<>();
    for (MultipartFile file : files) {
      String content = tika.parseToString(file.getInputStream());
      String docId = UUID.randomUUID().toString();
      String title = file.getOriginalFilename();
      documents.add(new RagDocument(docId, title, content, null));
    }
    orchestrator.ingest(new RagIngestRequest(documents));
    List<String> ids = documents.stream().map(RagDocument::id).toList();
    return ResponseEntity.accepted().body(ids);
  }

  @PostMapping("/search")
  public RagSearchResponse search(@Valid @RequestBody RagSearchRequest request) {
    return orchestrator.search(request);
  }

  @PostMapping("/delete")
  public ResponseEntity<Void> delete(@Valid @RequestBody RagDeleteRequest request) {
    orchestrator.deleteByDocumentId(request.documentId());
    return ResponseEntity.ok().build();
  }
}
