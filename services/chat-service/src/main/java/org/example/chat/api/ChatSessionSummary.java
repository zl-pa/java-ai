package org.example.chat.api;

import java.time.Instant;
import java.util.UUID;

public record ChatSessionSummary(UUID id, String title, Instant createdAt) {}
