package cr.chromapie.knowsitall.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import cr.chromapie.knowsitall.KnowsItAll;
import cr.chromapie.knowsitall.ModConfig;
import cr.chromapie.knowsitall.SystemPrompt;
import cr.chromapie.knowsitall.tool.ToolRegistry;

public class OpenAIClient {

    private static final Gson GSON = new GsonBuilder().create();
    private static volatile ExecutorService executor = createExecutor();

    private static final int CONNECT_TIMEOUT = 30000;
    private static final int READ_TIMEOUT = 120000;

    private static ExecutorService createExecutor() {
        return Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "KnowsItAll-API");
            t.setDaemon(true);
            return t;
        });
    }

    private static ExecutorService getExecutor() {
        if (executor == null || executor.isShutdown()) {
            executor = createExecutor();
        }
        return executor;
    }

    public static CompletableFuture<String> chat(UUID playerId, String userMessage, String context,
        Consumer<String> onSuccess, Consumer<String> onError) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                String response = sendChatRequest(playerId, userMessage, context);
                if (onSuccess != null) {
                    onSuccess.accept(response);
                }
                return response;
            } catch (Exception e) {
                String errorMsg = "API Error: " + e.getMessage();
                KnowsItAll.LOG.error("Chat request failed", e);
                if (onError != null) {
                    onError.accept(errorMsg);
                }
                return null;
            }
        }, getExecutor());
    }

    public static CompletableFuture<String> continueWithToolResult(UUID playerId, String toolResult,
        Consumer<String> onSuccess, Consumer<String> onError) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                String response = sendContinueRequest(playerId, toolResult);
                if (onSuccess != null) {
                    onSuccess.accept(response);
                }
                return response;
            } catch (Exception e) {
                String errorMsg = "API Error: " + e.getMessage();
                KnowsItAll.LOG.error("Continue request failed", e);
                if (onError != null) {
                    onError.accept(errorMsg);
                }
                return null;
            }
        }, getExecutor());
    }

    private static String sendChatRequest(UUID playerId, String userMessage, String context) throws IOException {
        if (!ModConfig.isConfigured()) {
            throw new IOException("API not configured. Use /knows config to set API key and URL.");
        }

        String fullUserMessage = userMessage;
        if (context != null && !context.isEmpty()) {
            fullUserMessage = "Context:\n" + context + "\n\nUser: " + userMessage;
        }

        String systemPrompt = SystemPrompt.get();
        int systemTokens = TokenEstimator.estimate(systemPrompt);
        int currentMsgTokens = TokenEstimator.estimate(fullUserMessage) + 4;

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(ChatMessage.system(systemPrompt));
        messages.addAll(ConversationManager.getHistoryWithinBudget(playerId, systemTokens, currentMsgTokens));
        messages.add(ChatMessage.user(fullUserMessage));

        String response = sendRequest(messages);

        ConversationManager.addUserMessage(playerId, userMessage);
        String cleanResponse = ToolRegistry.cleanResponse(response);
        if (ToolRegistry.isMeaningfulResponse(cleanResponse)) {
            ConversationManager.addAssistantMessage(playerId, cleanResponse);
        }

        return response;
    }

    private static String sendContinueRequest(UUID playerId, String toolResult) throws IOException {
        if (!ModConfig.isConfigured()) {
            throw new IOException("API not configured.");
        }

        String systemPrompt = SystemPrompt.get();
        int systemTokens = TokenEstimator.estimate(systemPrompt);
        String toolMessage = "[Tool Output - respond to user with this data]\n" + toolResult;
        int toolMsgTokens = TokenEstimator.estimate(toolMessage) + 4;

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(ChatMessage.system(systemPrompt));
        messages.addAll(ConversationManager.getHistoryWithinBudget(playerId, systemTokens, toolMsgTokens));
        messages.add(ChatMessage.user(toolMessage));

        String response = sendRequest(messages);

        ConversationManager.addToolResult(playerId, toolResult);
        String cleanResponse = ToolRegistry.cleanResponse(response);
        if (ToolRegistry.isMeaningfulResponse(cleanResponse)) {
            ConversationManager.addAssistantMessage(playerId, cleanResponse);
        }

        return response;
    }

    private static String sendRequest(List<ChatMessage> messages) throws IOException {
        ChatRequest request = new ChatRequest(
            ModConfig.getModel(),
            messages,
            ModConfig.getMaxTokens(),
            ModConfig.getTemperature());

        String jsonRequest = GSON.toJson(request);
        String endpoint = ModConfig.getApiUrl();
        if (!endpoint.endsWith("/")) {
            endpoint += "/";
        }
        endpoint += "chat/completions";

        URL url = new URL(endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        try {
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + ModConfig.getApiKey());
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(READ_TIMEOUT);
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonRequest.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            BufferedReader reader;

            if (responseCode >= 200 && responseCode < 300) {
                reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            } else {
                reader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
            }

            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            ChatResponse chatResponse = GSON.fromJson(response.toString(), ChatResponse.class);

            if (chatResponse.hasError()) {
                throw new IOException(chatResponse.getErrorMessage());
            }

            String content = chatResponse.getContent();
            if (content == null) {
                throw new IOException("Empty response from API");
            }

            return content;
        } finally {
            conn.disconnect();
        }
    }

    public static void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}
