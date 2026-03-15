package org.example.aicommon.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public record RagDocument(
    @NotBlank String id,
    String title,
    @NotBlank String content,
    Map<String, Object> metadata
) {}
