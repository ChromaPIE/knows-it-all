package cr.chromapie.knowsitall.tool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.entity.player.EntityPlayerMP;

public class ToolRegistry {

    private static final Pattern TOOL_PATTERN = Pattern.compile("\\[TOOL:([a-z_]+)((?::[^:\\]]+)*)]");
    private static final Map<String, Tool> tools = new HashMap<>();

    public static void register(String name, Tool tool) {
        tools.put(name, tool);
    }

    public static List<ToolCall> parseToolCalls(String response) {
        List<ToolCall> calls = new ArrayList<>();
        Matcher m = TOOL_PATTERN.matcher(response);
        while (m.find()) {
            String name = m.group(1);
            String[] args = m.group(2)
                .isEmpty() ? new String[0]
                    : m.group(2)
                        .substring(1)
                        .split(":");
            calls.add(new ToolCall(name, args, m.group(0)));
        }
        return calls;
    }

    public static String executeTools(EntityPlayerMP player, List<ToolCall> calls) {
        StringBuilder sb = new StringBuilder();
        for (ToolCall call : calls) {
            Tool tool = tools.get(call.name);
            if (tool != null) {
                String result = tool.execute(player, call.args);
                sb.append("\n=== Tool: ")
                    .append(call.name)
                    .append(" ===\n");
                sb.append(result)
                    .append("\n");
            }
        }
        return sb.toString();
    }

    public static String cleanResponse(String response) {
        String cleaned = TOOL_PATTERN.matcher(response)
            .replaceAll("")
            .trim();
        cleaned = cleaned.replaceAll("\\n{3,}", "\n\n");
        cleaned = cleaned.replaceAll("^(§[0-9a-fk-or])*\\s*\\n+", "$1");
        return cleaned;
    }

    public static boolean isMeaningfulResponse(String response) {
        if (response == null) return false;
        String stripped = response.replaceAll("§[0-9a-fk-or]", "").trim();
        return !stripped.isEmpty();
    }

    public static String getToolDescriptions() {
        StringBuilder sb = new StringBuilder();
        sb.append("§e§lAVAILABLE TOOLS§r (output [TOOL:name:args] to use):\n");
        for (Map.Entry<String, Tool> entry : tools.entrySet()) {
            sb.append("- [TOOL:")
                .append(entry.getKey());
            sb.append(
                entry.getValue()
                    .getArgFormat())
                .append("] - ");
            sb.append(
                entry.getValue()
                    .getDescription())
                .append("\n");
        }
        return sb.toString();
    }

    public static class ToolCall {

        public final String name;
        public final String[] args;
        public final String raw;

        public ToolCall(String name, String[] args, String raw) {
            this.name = name;
            this.args = args;
            this.raw = raw;
        }
    }

    public interface Tool {

        String execute(EntityPlayerMP player, String[] args);

        String getDescription();

        String getArgFormat();
    }
}
