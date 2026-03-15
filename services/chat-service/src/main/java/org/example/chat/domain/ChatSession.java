package org.example.chat.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
public class ChatSession {
  @Id
  @GeneratedValue
  private UUID id;

  private String title;

  private Instant createdAt = Instant.now();

  @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  private List<ChatMessage> messages = new ArrayList<>();

  public UUID getId() {
    return id;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public List<ChatMessage> getMessages() {
    return messages;
  }

  public void addMessage(ChatMessage message) {
    messages.add(message);
    message.setSession(this);
  }
}
