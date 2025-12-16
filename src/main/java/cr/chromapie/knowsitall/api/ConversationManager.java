package cr.chromapie.knowsitall.api;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import cr.chromapie.knowsitall.KnowsItAll;
import cr.chromapie.knowsitall.ModConfig;

public class ConversationManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static File saveFile;
    private static final Map<UUID, List<ChatMessage>> CONVERSATIONS = new HashMap<>();

    public static void init(File worldDir) {
        saveFile = new File(worldDir, "knowsitall_conversations.json");
        load();
    }

    public static void load() {
        if (saveFile == null || !saveFile.exists()) return;

        try (FileReader reader = new FileReader(saveFile)) {
            JsonObject root = new JsonParser().parse(reader).getAsJsonObject();
            CONVERSATIONS.clear();

            for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                UUID playerId = UUID.fromString(entry.getKey());
                JsonArray messagesJson = entry.getValue().getAsJsonArray();
                List<ChatMessage> messages = new ArrayList<>();

                for (JsonElement msgElem : messagesJson) {
                    JsonObject msgObj = msgElem.getAsJsonObject();
                    String role = msgObj.get("role").getAsString();
                    String content = msgObj.get("content").getAsString();
                    messages.add(new ChatMessage(role, content));
                }

                CONVERSATIONS.put(playerId, messages);
            }
            KnowsItAll.LOG.info("Loaded conversations for {} players", CONVERSATIONS.size());
        } catch (Exception e) {
            KnowsItAll.LOG.error("Failed to load conversations", e);
        }
    }

    public static void save() {
        if (saveFile == null) return;

        try {
            saveFile.getParentFile().mkdirs();
            JsonObject root = new JsonObject();

            for (Map.Entry<UUID, List<ChatMessage>> entry : CONVERSATIONS.entrySet()) {
                JsonArray messagesJson = new JsonArray();
                for (ChatMessage msg : entry.getValue()) {
                    JsonObject msgObj = new JsonObject();
                    msgObj.addProperty("role", msg.getRole());
                    msgObj.addProperty("content", msg.getContent());
                    messagesJson.add(msgObj);
                }
                root.add(entry.getKey().toString(), messagesJson);
            }

            try (FileWriter writer = new FileWriter(saveFile)) {
                GSON.toJson(root, writer);
            }
        } catch (IOException e) {
            KnowsItAll.LOG.error("Failed to save conversations", e);
        }
    }

    public static List<ChatMessage> getHistory(UUID playerId) {
        return CONVERSATIONS.computeIfAbsent(playerId, k -> new ArrayList<>());
    }

    public static void addUserMessage(UUID playerId, String content) {
        List<ChatMessage> history = getHistory(playerId);
        history.add(ChatMessage.user(content));
        trimByMessageCount(history);
        save();
    }

    public static void addAssistantMessage(UUID playerId, String content) {
        List<ChatMessage> history = getHistory(playerId);
        history.add(ChatMessage.assistant(content));
        trimByMessageCount(history);
        save();
    }

    public static void addToolResult(UUID playerId, String toolOutput) {
        List<ChatMessage> history = getHistory(playerId);
        history.add(ChatMessage.user("[Tool Output - respond to user with this data]\n" + toolOutput));
        trimByMessageCount(history);
        save();
    }

    public static void addToolHint(UUID playerId, String hint) {
        List<ChatMessage> history = getHistory(playerId);
        history.add(new ChatMessage("tool_hint", hint));
        trimByMessageCount(history);
        save();
    }

    public static List<ChatMessage> getHistoryWithinBudget(UUID playerId, int systemPromptTokens,
        int currentContextTokens) {
        List<ChatMessage> fullHistory = getHistory(playerId);
        if (fullHistory.isEmpty()) {
            return new ArrayList<>();
        }

        int maxInputTokens = ModConfig.getMaxInputTokens();
        int availableForHistory = maxInputTokens - systemPromptTokens - currentContextTokens;

        int minHistoryBudget = Math.min(availableForHistory, maxInputTokens / 5);
        availableForHistory = Math.max(availableForHistory, minHistoryBudget);

        if (availableForHistory <= 0) {
            return new ArrayList<>();
        }

        List<ChatMessage> result = new ArrayList<>();
        int usedTokens = 0;

        for (int i = fullHistory.size() - 1; i >= 0; i--) {
            ChatMessage msg = fullHistory.get(i);
            if (msg.isToolHint()) {
                continue;
            }
            int msgTokens = TokenEstimator.estimate(msg);

            if (usedTokens + msgTokens > availableForHistory) {
                break;
            }

            result.add(0, msg);
            usedTokens += msgTokens;
        }

        return result;
    }

    public static int estimateHistoryTokens(UUID playerId) {
        return TokenEstimator.estimate(getHistory(playerId));
    }

    public static void clear(UUID playerId) {
        CONVERSATIONS.remove(playerId);
        save();
    }

    public static void clearAll() {
        CONVERSATIONS.clear();
        save();
    }

    private static void trimByMessageCount(List<ChatMessage> history) {
        int maxMessages = ModConfig.getMaxHistoryMessages();
        while (history.size() > maxMessages) {
            history.remove(0);
        }
    }

    public static int getMessageCount(UUID playerId) {
        List<ChatMessage> history = CONVERSATIONS.get(playerId);
        return history == null ? 0 : history.size();
    }
}
