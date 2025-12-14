package cr.chromapie.knowsitall.knowledge;

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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import cr.chromapie.knowsitall.KnowsItAll;

public class KnowledgeBase {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting()
        .create();
    private static File saveFile;
    private static final Map<String, KnowledgeEntry> entries = new HashMap<>();

    public static void init(File worldDir) {
        saveFile = new File(worldDir, "knowsitall_kb.json");
        load();
    }

    public static void load() {
        if (saveFile == null || !saveFile.exists()) return;

        try (FileReader reader = new FileReader(saveFile)) {
            JsonObject root = new JsonParser().parse(reader)
                .getAsJsonObject();
            entries.clear();

            for (Map.Entry<String, JsonElement> e : root.entrySet()) {
                JsonObject obj = e.getValue()
                    .getAsJsonObject();
                String id = e.getKey();
                String type = obj.get("type")
                    .getAsString();
                String name = obj.get("name")
                    .getAsString();
                JsonObject data = obj.getAsJsonObject("data");
                long createdAt = obj.has("createdAt") ? obj.get("createdAt")
                    .getAsLong() : System.currentTimeMillis();
                entries.put(id, new KnowledgeEntry(id, type, name, data, createdAt));
            }
            KnowsItAll.LOG.info("Loaded {} knowledge entries", entries.size());
        } catch (Exception e) {
            KnowsItAll.LOG.error("Failed to load knowledge base", e);
        }
    }

    public static void save() {
        if (saveFile == null) return;

        try {
            saveFile.getParentFile()
                .mkdirs();
            JsonObject root = new JsonObject();

            for (KnowledgeEntry entry : entries.values()) {
                JsonObject obj = new JsonObject();
                obj.addProperty("type", entry.getType());
                obj.addProperty("name", entry.getName());
                obj.add("data", entry.getData());
                obj.addProperty("createdAt", entry.getCreatedAt());
                root.add(entry.getId(), obj);
            }

            try (FileWriter writer = new FileWriter(saveFile)) {
                GSON.toJson(root, writer);
            }
        } catch (IOException e) {
            KnowsItAll.LOG.error("Failed to save knowledge base", e);
        }
    }

    public static String generateId() {
        return UUID.randomUUID()
            .toString()
            .substring(0, 8);
    }

    public static KnowledgeEntry add(KnowledgeEntry entry) {
        entries.put(entry.getId(), entry);
        save();
        return entry;
    }

    public static boolean remove(String id) {
        if (entries.remove(id) != null) {
            save();
            return true;
        }
        return false;
    }

    public static boolean rename(String id, String newName) {
        KnowledgeEntry entry = entries.get(id);
        if (entry != null) {
            entry.setName(newName);
            save();
            return true;
        }
        return false;
    }

    public static KnowledgeEntry get(String id) {
        return entries.get(id);
    }

    public static KnowledgeEntry findByName(String name) {
        for (KnowledgeEntry entry : entries.values()) {
            if (entry.getName()
                .equalsIgnoreCase(name)) {
                return entry;
            }
        }
        return null;
    }

    public static KnowledgeEntry findByLocation(int dim, int x, int y, int z) {
        for (KnowledgeEntry entry : entries.values()) {
            if ("block".equals(entry.getType()) && entry.getDimension() == dim
                && entry.getX() == x
                && entry.getY() == y
                && entry.getZ() == z) {
                return entry;
            }
        }
        return null;
    }

    public static KnowledgeEntry findRecipeByItemKey(String itemKey) {
        for (KnowledgeEntry entry : entries.values()) {
            if ("recipe".equals(entry.getType()) && entry.getData()
                .has("itemKey")
                && entry.getData()
                    .get("itemKey")
                    .getAsString()
                    .equals(itemKey)) {
                return entry;
            }
        }
        return null;
    }

    public static List<KnowledgeEntry> getAll() {
        return new ArrayList<>(entries.values());
    }

    public static List<KnowledgeEntry> getByType(String type) {
        List<KnowledgeEntry> result = new ArrayList<>();
        for (KnowledgeEntry entry : entries.values()) {
            if (entry.getType()
                .equals(type)) {
                result.add(entry);
            }
        }
        return result;
    }

    public static int size() {
        return entries.size();
    }

    public static int sizeByType(String type) {
        int count = 0;
        for (KnowledgeEntry entry : entries.values()) {
            if (entry.getType()
                .equals(type)) count++;
        }
        return count;
    }

    public static void clear() {
        entries.clear();
        save();
    }

    public static void clearByType(String type) {
        entries.values()
            .removeIf(
                e -> e.getType()
                    .equals(type));
        save();
    }
}
