package cr.chromapie.knowsitall.api;

public class ChatMessage {

    private final String role;
    private final String content;

    public ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public static ChatMessage system(String content) {
        return new ChatMessage("system", content);
    }

    public static ChatMessage user(String content) {
        return new ChatMessage("user", content);
    }

    public static ChatMessage assistant(String content) {
        return new ChatMessage("assistant", content);
    }

    public String getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    public boolean isToolResult() {
        if (content == null) return false;
        return content.startsWith("[Tool Output") || ("system".equals(role) && content.startsWith("Tool results:"));
    }

    public boolean isDisplayable() {
        if (isToolResult()) return false;
        return "user".equals(role) || "assistant".equals(role) || "tool_hint".equals(role);
    }

    public boolean isToolHint() {
        return "tool_hint".equals(role);
    }
}
