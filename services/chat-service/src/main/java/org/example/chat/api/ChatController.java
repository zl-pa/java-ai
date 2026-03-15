package org.example.chat.api;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.example.aicommon.dto.ChatMessageDto;
import org.example.aicommon.dto.ChatRequest;
import org.example.aicommon.dto.ChatResponse;
import org.example.chat.ChatOrchestrator;
import org.example.chat.domain.ChatSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chats")
public class ChatController {
  private final ChatOrchestrator orchestrator;

  public ChatController(ChatOrchestrator orchestrator) {
    this.orchestrator = orchestrator;
  }

  @PostMapping
  public ChatSessionSummary createChat() {
    ChatSession session = orchestrator.createSession();
    return new ChatSessionSummary(session.getId(), session.getTitle(), session.getCreatedAt());
  }

  @GetMapping
  public List<ChatSessionSummary> listChats() {
    return orchestrator.listSessions().stream()
        .map(session -> new ChatSessionSummary(session.getId(), session.getTitle(), session.getCreatedAt()))
        .toList();
  }

  @GetMapping("/{chatId}/messages")
  public List<ChatMessageDto> listMessages(@PathVariable UUID chatId) {
    return orchestrator.listMessages(chatId);
  }

  @PostMapping("/{chatId}/messages")
  public ChatResponse postMessage(@PathVariable UUID chatId, @Valid @RequestBody ChatRequest request) {
    return orchestrator.postMessage(chatId, request.content());
  }
}
