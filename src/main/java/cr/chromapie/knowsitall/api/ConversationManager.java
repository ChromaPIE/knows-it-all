package cr.chromapie.knowsitall.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ConversationManager {

    private static final Map<UUID, List<ChatMessage>> CONVERSATIONS = new HashMap<>();
    private static final int MAX_HISTORY = 50;

    public static List<ChatMessage> getHistory(UUID playerId) {
        return CONVERSATIONS.computeIfAbsent(playerId, k -> new ArrayList<>());
    }

    public static void addUserMessage(UUID playerId, String content) {
        List<ChatMessage> history = getHistory(playerId);
        history.add(ChatMessage.user(content));
        trimHistory(history);
    }

    public static void addAssistantMessage(UUID playerId, String content) {
        List<ChatMessage> history = getHistory(playerId);
        history.add(ChatMessage.assistant(content));
        trimHistory(history);
    }

    public static void addToolResult(UUID playerId, String toolOutput) {
        List<ChatMessage> history = getHistory(playerId);
        history.add(ChatMessage.system("Tool results:\n" + toolOutput));
        trimHistory(history);
    }

    public static void clear(UUID playerId) {
        CONVERSATIONS.remove(playerId);
    }

    public static void clearAll() {
        CONVERSATIONS.clear();
    }

    private static void trimHistory(List<ChatMessage> history) {
        while (history.size() > MAX_HISTORY) {
            history.remove(0);
        }
    }

    public static int getMessageCount(UUID playerId) {
        List<ChatMessage> history = CONVERSATIONS.get(playerId);
        return history == null ? 0 : history.size();
    }
}
