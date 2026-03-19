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
import org.example.chat.history.HistoryWindowStrategy;
import org.example.chat.llm.LlmClient;
import org.example.chat.llm.LlmMessage;
import org.example.chat.prompt.PromptBuilder;
import org.example.chat.rag.RagClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

/**
 * Chat 领域编排器。
 *
 * <p>职责：
 * 1. 管理会话与消息持久化；
 * 2. 调用 RAG 服务检索知识；
 * 3. 构建 Prompt 并调用本地 LLM；
 * 4. 返回最终回答与命中的知识片段。
 */
@Service
public class ChatOrchestrator {
  private static final Logger log = LoggerFactory.getLogger(ChatOrchestrator.class);

  private final ChatSessionRepository sessionRepository;
  private final ChatMessageRepository messageRepository;
  private final RagClient ragClient;
  private final LlmClient llmClient;
  private final ChatServiceProperties properties;
  private final PromptBuilder promptBuilder;
  private final HistoryWindowStrategy historyWindowStrategy;

  public ChatOrchestrator(
      ChatSessionRepository sessionRepository,
      ChatMessageRepository messageRepository,
      RagClient ragClient,
      LlmClient llmClient,
      ChatServiceProperties properties,
      PromptBuilder promptBuilder,
      HistoryWindowStrategy historyWindowStrategy) {
    this.sessionRepository = sessionRepository;
    this.messageRepository = messageRepository;
    this.ragClient = ragClient;
    this.llmClient = llmClient;
    this.properties = properties;
    this.promptBuilder = promptBuilder;
    this.historyWindowStrategy = historyWindowStrategy;
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
    ChatSession session = getSession(sessionId);

    // 1) 落库用户消息。
    ChatMessage userMessage = new ChatMessage();
    userMessage.setRole(MessageRole.USER);
    userMessage.setContent(content);
    userMessage.setSession(session);
    messageRepository.save(userMessage);

    if ("New Chat".equals(session.getTitle())) {
      session.setTitle(suggestTitle(content));
      sessionRepository.save(session);
    }

    PromptContext promptContext = buildPromptContext(sessionId, content);

    // 5) 调用本地 LLM。
    String assistantReply = llmClient.chat(promptContext.messages());

    // 6) 落库模型回复。
    saveAssistantMessage(session, assistantReply);

    return new ChatResponse(assistantReply, promptContext.chunks());
  }

  public Flux<String> postMessageStream(UUID sessionId, String content) {
    ChatSession session = getSession(sessionId);

    ChatMessage userMessage = new ChatMessage();
    userMessage.setRole(MessageRole.USER);
    userMessage.setContent(content);
    userMessage.setSession(session);
    messageRepository.save(userMessage);

    if ("New Chat".equals(session.getTitle())) {
      session.setTitle(suggestTitle(content));
      sessionRepository.save(session);
    }

    PromptContext promptContext = buildPromptContext(sessionId, content);
    StringBuilder assistantReplyBuilder = new StringBuilder();

    return llmClient
        .chatStream(promptContext.messages())
        .doOnNext(assistantReplyBuilder::append)
        .doOnComplete(() -> saveAssistantMessage(session, assistantReplyBuilder.toString()));
  }

  private PromptContext buildPromptContext(UUID sessionId, String content) {
    // 2) 加载并裁剪历史，避免上下文无限增长。
    List<ChatMessage> history = messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
    history.sort(Comparator.comparing(ChatMessage::getCreatedAt));
    List<ChatMessage> trimmedHistory =
        historyWindowStrategy.trim(history, properties.historyMaxMessages());

    // 3) 检索本地知识库，作为回答依据。
    RagSearchResponse ragResponse = ragClient.search(content, properties.ragTopK());
    List<RagChunk> chunks = ragResponse.chunks();

    // 4) 构建系统提示词并组装 LLM 消息。
    String systemPrompt = promptBuilder.buildSystemPrompt(chunks);
    List<LlmMessage> messages = new ArrayList<>();
    messages.add(new LlmMessage("system", systemPrompt));
    for (ChatMessage message : trimmedHistory) {
      messages.add(new LlmMessage(toRole(message.getRole()), message.getContent()));
    }
      int historyCharLength =
              trimmedHistory.stream().mapToInt(message -> safeLength(message.getContent())).sum();
      int ragCharLength = chunks.stream().mapToInt(chunk -> safeLength(chunk.text())).sum();
      log.info(
              "Preparing llmClient.chat: messagesCount={}, systemPromptChars={}, historyChars={}, ragChunksCount={}, ragChunksChars={}",
              messages.size(),
              systemPrompt.length(),
              historyCharLength,
              chunks.size(),
              ragCharLength);

    return new PromptContext(messages, chunks);
  }

  private void saveAssistantMessage(ChatSession session, String assistantReply) {
    ChatMessage assistantMessage = new ChatMessage();
    assistantMessage.setRole(MessageRole.ASSISTANT);
    assistantMessage.setContent(assistantReply);
    assistantMessage.setSession(session);
    messageRepository.save(assistantMessage);
  }

  private ChatSession getSession(UUID sessionId) {
    return sessionRepository
        .findById(sessionId)
        .orElseThrow(() -> new IllegalArgumentException("Chat session not found"));
  }

  private record PromptContext(List<LlmMessage> messages, List<RagChunk> chunks) {}

  private String toRole(MessageRole role) {
    return switch (role) {
      case SYSTEM -> "system";
      case USER -> "user";
      case ASSISTANT -> "assistant";
    };
  }

  private int safeLength(String text) {
    return text == null ? 0 : text.length();
  }

  /**
   * 根据首条问题生成会话标题。
   */
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
