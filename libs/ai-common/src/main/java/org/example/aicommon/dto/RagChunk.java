package org.example.aicommon.dto;

import java.util.Map;

public record RagChunk(String id, String text, String title, double score, Map<String, Object> metadata) {}
