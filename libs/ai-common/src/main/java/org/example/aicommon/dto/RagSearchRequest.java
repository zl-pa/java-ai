package org.example.aicommon.dto;

import jakarta.validation.constraints.NotBlank;

public record RagSearchRequest(@NotBlank String query, int topK) {}
