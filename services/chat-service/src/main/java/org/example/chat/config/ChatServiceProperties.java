package org.example.chat.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "chat")
public record ChatServiceProperties(int historyMaxMessages, int ragTopK, int ragChunkMaxChars) {}
