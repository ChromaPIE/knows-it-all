package cr.chromapie.knowsitall.api;

import java.util.List;

public class ChatRequest {

    private final String model;
    private final List<ChatMessage> messages;
    private final int max_tokens;
    private final double temperature;

    public ChatRequest(String model, List<ChatMessage> messages, int maxTokens, double temperature) {
        this.model = model;
        this.messages = messages;
        this.max_tokens = maxTokens;
        this.temperature = temperature;
    }
}
