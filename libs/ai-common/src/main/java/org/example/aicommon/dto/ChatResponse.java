package org.example.aicommon.dto;

import java.util.List;

public record ChatResponse(String assistantMessage, List<RagChunk> sources) {}
