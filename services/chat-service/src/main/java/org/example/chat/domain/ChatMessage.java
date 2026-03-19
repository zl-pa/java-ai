package org.example.chat.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
public class ChatMessage {
  @Id
  @GeneratedValue
  private UUID id;

  @Enumerated(EnumType.STRING)
  private MessageRole role;

  @Lob
  @Column(columnDefinition = "TEXT")
  private String content;

  private Instant createdAt = Instant.now();

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "session_id")
  private ChatSession session;

  public UUID getId() {
    return id;
  }

  public MessageRole getRole() {
    return role;
  }

  public void setRole(MessageRole role) {
    this.role = role;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public ChatSession getSession() {
    return session;
  }

  public void setSession(ChatSession session) {
    this.session = session;
  }
}
