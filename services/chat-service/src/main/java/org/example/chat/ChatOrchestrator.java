package org.example.chat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.example.aicommon.dto.ChatMessageDto;
import org.example.aicommon.dto.ChatResponse;
import org.example.aicommon.dto.RagChunk;
import org.example.aicommon.dto.RagSearchResponse;
import org.example.chat.config.ChatServiceProperties;
import org.example.chat.domain.ChatMessage;
import org.example.chat.domain.ChatMessageRepository;
import org.example.chat.domain.ChatSession;
import org.example.chat.domain.ChatSessionRepository;
import org.example.chat.domain.MessageRole;
import org.example.chat.llm.LlmClient;
import org.example.chat.llm.LlmMessage;
import org.example.chat.rag.RagClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatOrchestrator {
  private final ChatSessionRepository sessionRepository;
  private final ChatMessageRepository messageRepository;
  private final RagClient ragClient;
  private final LlmClient llmClient;
  private final ChatServiceProperties properties;

  public ChatOrchestrator(
      ChatSessionRepository sessionRepository,
      ChatMessageRepository messageRepository,
      RagClient ragClient,
      LlmClient llmClient,
      ChatServiceProperties properties) {
    this.sessionRepository = sessionRepository;
    this.messageRepository = messageRepository;
    this.ragClient = ragClient;
    this.llmClient = llmClient;
    this.properties = properties;
  }

  public ChatSession createSession() {
    ChatSession session = new ChatSession();
    session.setTitle("New Chat");
    return sessionRepository.save(session);
  }

  public List<ChatSession> listSessions() {
    return sessionRepository.findAll();
  }

  public List<ChatMessageDto> listMessages(UUID sessionId) {
    return messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId).stream()
        .map(
            message ->
                new ChatMessageDto(
                    message.getRole().name().toLowerCase(Locale.ROOT),
                    message.getContent(),
                    message.getCreatedAt()))
        .toList();
  }

  @Transactional
  public ChatResponse postMessage(UUID sessionId, String content) {
    ChatSession session =
        sessionRepository
            .findById(sessionId)
            .orElseThrow(() -> new IllegalArgumentException("Chat session not found"));

    ChatMessage userMessage = new ChatMessage();
    userMessage.setRole(MessageRole.USER);
    userMessage.setContent(content);
    session.addMessage(userMessage);
    sessionRepository.save(session);

    if ("New Chat".equals(session.getTitle())) {
      session.setTitle(suggestTitle(content));
    }

    // Load history to keep multi-turn context.
    List<ChatMessage> history =
        messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
    history.sort(Comparator.comparing(ChatMessage::getCreatedAt));
    List<ChatMessage> trimmedHistory =
        history.size() > properties.historyMaxMessages()
            ? history.subList(Math.max(history.size() - properties.historyMaxMessages(), 0), history.size())
            : history;

    // Retrieve local knowledge to ground the answer.
    RagSearchResponse ragResponse = ragClient.search(content, properties.ragTopK());
    List<RagChunk> chunks = ragResponse.chunks();

    // Build system prompt with RAG context.
    String systemPrompt = buildSystemPrompt(chunks);
    List<LlmMessage> messages = new ArrayList<>();
    messages.add(new LlmMessage("system", systemPrompt));
    for (ChatMessage message : trimmedHistory) {
      messages.add(new LlmMessage(toRole(message.getRole()), message.getContent()));
    }

    // Call local LLM via the configured API.
    String assistantReply = llmClient.chat(messages);

    ChatMessage assistantMessage = new ChatMessage();
    assistantMessage.setRole(MessageRole.ASSISTANT);
    assistantMessage.setContent(assistantReply);
    session.addMessage(assistantMessage);
    sessionRepository.save(session);

    return new ChatResponse(assistantReply, chunks);
  }

  private String toRole(MessageRole role) {
    return switch (role) {
      case SYSTEM -> "system";
      case USER -> "user";
      case ASSISTANT -> "assistant";
    };
  }

  private String buildSystemPrompt(List<RagChunk> chunks) {
    if (chunks == null || chunks.isEmpty()) {
      return "You are a helpful AI assistant. If you do not know the answer, say you do not know.";
    }
    StringBuilder builder = new StringBuilder();
    builder.append("You are a helpful AI assistant. Use the context below when relevant.\n\n");
    int index = 1;
    for (RagChunk chunk : chunks) {
      builder.append("[").append(index).append("] ");
      if (chunk.title() != null && !chunk.title().isBlank()) {
        builder.append(chunk.title()).append(": ");
      }
      builder.append(chunk.text()).append("\n");
      index++;
    }
    builder.append("\nIf the context does not contain the answer, say you do not know.");
    return builder.toString();
  }

  private String suggestTitle(String content) {
    String trimmed = content.trim();
    if (trimmed.isEmpty()) {
      return "New Chat";
    }
    String[] words = trimmed.split("\\s+");
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < Math.min(words.length, 6); i++) {
      if (i > 0) {
        builder.append(" ");
      }
      builder.append(words[i]);
    }
    return builder.toString();
  }
}
