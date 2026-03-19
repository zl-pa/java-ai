package org.example.chat.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import java.util.List;
import javax.sql.DataSource;
import org.example.aicommon.dto.RagSearchResponse;
import org.example.chat.domain.ChatMessage;
import org.example.chat.domain.ChatMessageRepository;
import org.example.chat.domain.ChatSession;
import org.example.chat.domain.ChatSessionRepository;
import org.example.chat.domain.MessageRole;
import org.example.chat.llm.LlmClient;
import org.example.chat.rag.RagClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.FluxExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class ChatControllerStreamPersistenceTest {

  @Autowired private WebTestClient webTestClient;

  @Autowired private ChatSessionRepository sessionRepository;

  @Autowired private ChatMessageRepository messageRepository;

  @MockBean private RagClient ragClient;

  @MockBean private LlmClient llmClient;

  @Autowired private DataSource dataSource;

  @Test
  void postMessageStream_persistsUserAndAssistantMessages_withoutLazyInitializationException() {
    ChatSession session = new ChatSession();
    session.setTitle("New Chat");
    session = sessionRepository.save(session);

    given(ragClient.search(anyString(), anyInt())).willReturn(new RagSearchResponse(List.of()));
    given(llmClient.chatStream(any())).willReturn(Flux.just("assistant ", "reply"));

    FluxExchangeResult<ServerSentEvent<String>> result =
        webTestClient
            .post()
            .uri("/api/chats/{chatId}/messages/stream", session.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{\"content\":\"Hello stream\"}")
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(new ParameterizedTypeReference<>() {});

    StepVerifier.create(result.getResponseBody())
        .expectNextMatches(event -> "delta".equals(event.event()) && "assistant ".equals(event.data()))
        .expectNextMatches(event -> "delta".equals(event.event()) && "reply".equals(event.data()))
        .expectNextMatches(event -> "done".equals(event.event()) && "[DONE]".equals(event.data()))
        .verifyComplete();

    List<ChatMessage> storedMessages =
        messageRepository.findBySessionIdOrderByCreatedAtAsc(session.getId());
    assertThat(storedMessages).hasSize(2);
    assertThat(storedMessages)
        .extracting(ChatMessage::getRole, ChatMessage::getContent)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple(MessageRole.USER, "Hello stream"),
            org.assertj.core.groups.Tuple.tuple(MessageRole.ASSISTANT, "assistant reply"));
  }

  @Test
  void jpaSchema_keepsForeignKeyOnChatMessageSessionId_only() throws Exception {
    try (var connection = dataSource.getConnection()) {
      boolean hasSessionIdColumn = false;
      try (var columns = connection.getMetaData().getColumns(null, null, "CHAT_MESSAGE", "SESSION_ID")) {
        hasSessionIdColumn = columns.next();
      }
      assertThat(hasSessionIdColumn).isTrue();

      boolean hasSessionForeignKey = false;
      try (var foreignKeys = connection.getMetaData().getImportedKeys(null, null, "CHAT_MESSAGE")) {
        while (foreignKeys.next()) {
          String fkColumn = foreignKeys.getString("FKCOLUMN_NAME");
          String pkTable = foreignKeys.getString("PKTABLE_NAME");
          if ("SESSION_ID".equalsIgnoreCase(fkColumn) && "CHAT_SESSION".equalsIgnoreCase(pkTable)) {
            hasSessionForeignKey = true;
            break;
          }
        }
      }
      assertThat(hasSessionForeignKey).isTrue();
    }
  }
}
