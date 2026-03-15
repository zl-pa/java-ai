package org.example.aicommon.dto;

import jakarta.validation.constraints.NotBlank;

public record RagDeleteRequest(@NotBlank String documentId) {}
