package cr.chromapie.knowsitall;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public final class ModConfig {

    private static Configuration config;

    private static String apiUrl = "https://api.openai.com/v1";
    private static String apiKey = "";
    private static String model = "gpt-4o";
    private static String userPrompt = "";
    private static int maxTokens = 4096;
    private static double temperature = 0.7;
    private static boolean promptOnMismatch = true;
    private static int defaultScanRange = 5;
    private static int contextWindow = 128000;
    private static int reservedOutputTokens = 8192;
    private static int maxHistoryMessages = 100;

    private ModConfig() {}

    public static void init(File configFile) {
        if (config != null) return;
        config = new Configuration(configFile);
        load();
    }

    public static void reload() {
        if (config == null) return;
        config.load();
        load();
    }

    public static void load() {
        config.load();

        apiUrl = config.getString("apiUrl", "api", apiUrl, "OpenAI-compatible API endpoint URL");

        apiKey = config.getString("apiKey", "api", apiKey, "API authentication token");

        model = config.getString("model", "api", model, "Model identifier");

        maxTokens = config.getInt("maxTokens", "api", maxTokens, 128, 16384, "Max response tokens");

        temperature = config.getFloat("temperature", "api", (float) temperature, 0.0f, 2.0f, "Response randomness");

        contextWindow = config
            .getInt("contextWindow", "api", contextWindow, 4096, 200000, "Model context window size in tokens");

        reservedOutputTokens = config.getInt(
            "reservedOutputTokens",
            "api",
            reservedOutputTokens,
            1024,
            32768,
            "Tokens reserved for model output");

        maxHistoryMessages = config.getInt(
            "maxHistoryMessages",
            "conversation",
            maxHistoryMessages,
            10,
            500,
            "Maximum conversation history messages to keep");

        userPrompt = config.getString(
            "userPrompt",
            "prompt",
            userPrompt,
            "Additional user instructions (optional, appended to system prompt)");

        promptOnMismatch = config.getBoolean(
            "promptOnMismatch",
            "knowledge",
            promptOnMismatch,
            "Prompt on KB entry mismatch (false = auto-remove)");

        defaultScanRange = config.getInt(
            "defaultScanRange",
            "context",
            defaultScanRange,
            0,
            16,
            "Default range to scan for containers (0 = disabled)");

        config.save();
    }

    public static void save() {
        if (config == null) return;
        config.save();
    }

    public static String getApiUrl() {
        return apiUrl;
    }

    public static void setApiUrl(String url) {
        apiUrl = url;
        config.getCategory("api")
            .get("apiUrl")
            .set(url);
        save();
    }

    public static String getApiKey() {
        return apiKey;
    }

    public static void setApiKey(String key) {
        apiKey = key;
        config.getCategory("api")
            .get("apiKey")
            .set(key);
        save();
    }

    public static String getModel() {
        return model;
    }

    public static void setModel(String modelId) {
        model = modelId;
        config.getCategory("api")
            .get("model")
            .set(modelId);
        save();
    }

    public static String getUserPrompt() {
        return userPrompt;
    }

    public static int getMaxTokens() {
        return maxTokens;
    }

    public static double getTemperature() {
        return temperature;
    }

    public static boolean shouldPromptOnMismatch() {
        return promptOnMismatch;
    }

    public static int getDefaultScanRange() {
        return defaultScanRange;
    }

    public static int getContextWindow() {
        return contextWindow;
    }

    public static int getReservedOutputTokens() {
        return reservedOutputTokens;
    }

    public static int getMaxInputTokens() {
        return contextWindow - reservedOutputTokens;
    }

    public static int getMaxHistoryMessages() {
        return maxHistoryMessages;
    }

    public static boolean isConfigured() {
        return !apiKey.isEmpty() && !apiUrl.isEmpty();
    }
}
