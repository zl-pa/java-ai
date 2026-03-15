package org.example.aicommon.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(@NotBlank String content) {}
