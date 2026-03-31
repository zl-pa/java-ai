package org.example.rag.config;

import org.example.rag.text.TextChunker;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChunkingConfig {

  @Bean
  public TextChunker textChunker(RagProperties properties) {
    return new TextChunker(properties.chunkSize(), properties.chunkOverlap());
  }
}
