package org.example.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rag")
public record RagProperties(String qdrantBaseUrl, String collection, int chunkSize, int chunkOverlap) {}
