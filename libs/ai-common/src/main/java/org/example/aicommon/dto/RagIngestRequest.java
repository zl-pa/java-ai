package org.example.aicommon.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record RagIngestRequest(@NotEmpty List<RagDocument> documents) {}
