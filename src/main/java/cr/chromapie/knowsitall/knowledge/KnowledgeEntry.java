package cr.chromapie.knowsitall.knowledge;

import com.google.gson.JsonObject;

public class KnowledgeEntry {

    private final String id;
    private final String type;
    private String name;
    private final JsonObject data;
    private final long createdAt;

    public KnowledgeEntry(String id, String type, String name, JsonObject data) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.data = data;
        this.createdAt = System.currentTimeMillis();
    }

    public KnowledgeEntry(String id, String type, String name, JsonObject data, long createdAt) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.data = data;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public JsonObject getData() {
        return data;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    // Block helpers
    public int getX() {
        return data.has("x") ? data.get("x")
            .getAsInt() : 0;
    }

    public int getY() {
        return data.has("y") ? data.get("y")
            .getAsInt() : 0;
    }

    public int getZ() {
        return data.has("z") ? data.get("z")
            .getAsInt() : 0;
    }

    public int getDimension() {
        return data.has("dim") ? data.get("dim")
            .getAsInt() : 0;
    }

    public String getBlockId() {
        return data.has("blockId") ? data.get("blockId")
            .getAsString() : "";
    }

    public String getLocationString() {
        if (!"block".equals(type)) return "";
        return String.format("dim%d @ [%d, %d, %d]", getDimension(), getX(), getY(), getZ());
    }

    public static JsonObject blockData(int dim, int x, int y, int z, String blockId, int meta) {
        JsonObject obj = new JsonObject();
        obj.addProperty("dim", dim);
        obj.addProperty("x", x);
        obj.addProperty("y", y);
        obj.addProperty("z", z);
        obj.addProperty("blockId", blockId);
        obj.addProperty("meta", meta);
        return obj;
    }

    public static JsonObject recipeData(String itemKey, JsonObject recipeId) {
        JsonObject obj = new JsonObject();
        obj.addProperty("itemKey", itemKey);
        obj.add("recipeId", recipeId);
        return obj;
    }
}
