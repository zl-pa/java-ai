package org.example.rag.api;

import jakarta.validation.Valid;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
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
            throws IOException { // 移除 TikaException，因为纯文本不需要它

        List<RagDocument> documents = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue;
            }

            String originalFilename = file.getOriginalFilename();
            String content;

            // 【核心修改】针对 txt/md/csv 文件，直接读取字节转字符串，绕过 Tika
            if (originalFilename != null && (originalFilename.endsWith(".txt")
                    || originalFilename.endsWith(".md")
                    || originalFilename.endsWith(".csv")
                    || originalFilename.endsWith(".json")
                    || "text/plain".equals(file.getContentType()))) {

                // 尝试 UTF-8，如果业务涉及 GBK 需调整
                content = new String(file.getBytes(), java.nio.charset.StandardCharsets.UTF_8);

                // 可选：如果上面 UTF-8 乱码，可以尝试自动检测或使用 default charset
                // content = new String(file.getBytes());
            } else {
                // 其他格式 (pdf, docx 等) 继续使用 Tika
                try (InputStream inputStream = file.getInputStream()) {
                    content = tika.parseToString(inputStream);
                } catch (Exception e) {
                    throw new RuntimeException("解析文件失败: " + originalFilename, e);
                }
            }

            // 双重检查：如果内容依然为空（可能是空文件）
            if (content == null || content.trim().isEmpty()) {
                System.err.println("警告：文件内容为空 - " + originalFilename);
                // 根据需求决定是跳过还是报错
                // continue;
            }

            String docId = java.util.UUID.randomUUID().toString();
            documents.add(new RagDocument(docId, originalFilename, content, null));
        }

        if (documents.isEmpty()) {
            return ResponseEntity.badRequest().body(null);
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
