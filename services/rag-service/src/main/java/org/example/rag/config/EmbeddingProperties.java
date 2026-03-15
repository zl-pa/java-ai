package org.example.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "embedding")
public record EmbeddingProperties(String baseUrl, String model) {}
