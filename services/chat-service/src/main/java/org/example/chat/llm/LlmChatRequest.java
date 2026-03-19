package org.example.chat.llm;

import java.util.List;

public record LlmChatRequest(
    String model,
    List<LlmMessage> messages,
    Double temperature,
    Boolean stream
) {}
