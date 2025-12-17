package cr.chromapie.knowsitall.tool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.registry.GameRegistry;
import cr.chromapie.knowsitall.knowledge.KnowledgeBase;
import cr.chromapie.knowsitall.knowledge.KnowledgeEntry;
import cr.chromapie.knowsitall.tool.ToolRegistry.Tool;
import cr.chromapie.knowsitall.util.WorldDataReader;

public class Tools {

    public static void registerAll() {
        ToolRegistry.register("scan", new ScanTool());
        ToolRegistry.register("container", new ContainerTool());
        ToolRegistry.register("kb", new KnowledgeBaseTool());
        ToolRegistry.register("kb_add", new KbAddTool());
        ToolRegistry.register("block", new BlockTool());
        ToolRegistry.register("item_search", new ItemSearchTool());
        ToolRegistry.register("recipe", new RecipeTool());
        ToolRegistry.register("kb_note", new KbNoteTool());
    }

    static class ScanTool implements Tool {

        @Override
        public String execute(EntityPlayerMP player, String[] args) {
            if (args.length < 2) return "Error: need filter and range";
            String filter = args[0].toLowerCase();
            int range;
            try {
                range = Math.min(16, Math.max(1, Integer.parseInt(args[1])));
            } catch (NumberFormatException e) {
                return "Error: invalid range";
            }

            World world = player.worldObj;
            int px = (int) player.posX, py = (int) player.posY, pz = (int) player.posZ;
            Map<String, Integer> counts = new HashMap<>();
            Map<String, String> nearest = new HashMap<>();
            int rangeSq = range * range;

            for (int x = px - range; x <= px + range; x++) {
                for (int y = py - range; y <= py + range; y++) {
                    for (int z = pz - range; z <= pz + range; z++) {
                        int dx = x - px, dy = y - py, dz = z - pz;
                        if (dx * dx + dy * dy + dz * dz > rangeSq) continue;

                        Block block = world.getBlock(x, y, z);
                        if (block.isAir(world, x, y, z)) continue;

                        String name = block.getLocalizedName()
                            .toLowerCase();
                        GameRegistry.UniqueIdentifier uid = GameRegistry.findUniqueIdentifierFor(block);
                        String id = uid != null ? uid.toString()
                            .toLowerCase() : "";

                        if (name.contains(filter) || id.contains(filter)) {
                            String key = block.getLocalizedName();
                            counts.merge(key, 1, Integer::sum);
                            if (!nearest.containsKey(key)) {
                                nearest.put(key, "[" + x + "," + y + "," + z + "]");
                            }
                        }
                    }
                }
            }

            if (counts.isEmpty()) {
                return "No '" + filter + "' found within " + range + " blocks.";
            }

            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, Integer> e : counts.entrySet()) {
                sb.append(e.getKey())
                    .append(": ")
                    .append(e.getValue())
                    .append(" (nearest: ")
                    .append(nearest.get(e.getKey()))
                    .append(")\n");
            }
            return sb.toString();
        }

        @Override
        public String getDescription() {
            return "Scan nearby blocks by name";
        }

        @Override
        public String getArgFormat() {
            return ":filter:range";
        }
    }

    static class ContainerTool implements Tool {

        @Override
        public String execute(EntityPlayerMP player, String[] args) {
            if (args.length < 3) return "Error: need x, y, z";
            try {
                int x = Integer.parseInt(args[0]);
                int y = Integer.parseInt(args[1]);
                int z = Integer.parseInt(args[2]);

                TileEntity te = player.worldObj.getTileEntity(x, y, z);
                if (te instanceof IInventory) {
                    return WorldDataReader.getInventoryContents((IInventory) te);
                }
                return "No container at [" + x + "," + y + "," + z + "]";
            } catch (NumberFormatException e) {
                return "Error: invalid coordinates";
            }
        }

        @Override
        public String getDescription() {
            return "Get container contents at coords";
        }

        @Override
        public String getArgFormat() {
            return ":x:y:z";
        }
    }

    static class KnowledgeBaseTool implements Tool {

        @Override
        public String execute(EntityPlayerMP player, String[] args) {
            if (args.length < 1) return "Error: need id or 'list'";

            if (args[0].equals("list")) {
                StringBuilder sb = new StringBuilder("Knowledge Base entries:\n");
                for (KnowledgeEntry e : KnowledgeBase.getAll()) {
                    sb.append("- ")
                        .append(e.getId())
                        .append(" [")
                        .append(e.getType())
                        .append("]: ")
                        .append(e.getName())
                        .append("\n");
                }
                return sb.toString();
            }

            KnowledgeEntry entry = KnowledgeBase.get(args[0]);
            if (entry == null) entry = KnowledgeBase.findByName(args[0]);
            if (entry == null) return "Entry not found: " + args[0];

            return formatEntry(player, entry);
        }

        private String formatEntry(EntityPlayerMP player, KnowledgeEntry entry) {
            StringBuilder sb = new StringBuilder();
            sb.append("=== ")
                .append(entry.getName())
                .append(" [")
                .append(entry.getType())
                .append("] ===\n");

            switch (entry.getType()) {
                case "block":
                    sb.append("Location: dim")
                        .append(entry.getDimension())
                        .append(" @ [")
                        .append(entry.getX())
                        .append(", ")
                        .append(entry.getY())
                        .append(", ")
                        .append(entry.getZ())
                        .append("]\n\n");
                    sb.append(
                        WorldDataReader.getFullDataForAI(player.worldObj, entry.getX(), entry.getY(), entry.getZ()));
                    break;

                case "recipe":
                    com.google.gson.JsonObject data = entry.getData();
                    if (data.has("recipe")) {
                        com.google.gson.JsonObject recipe = data.getAsJsonObject("recipe");
                        sb.append("Handler: ")
                            .append(
                                recipe.has("handler") ? recipe.get("handler")
                                    .getAsString() : "Unknown")
                            .append("\n");
                        if (recipe.has("ingredients")) {
                            sb.append("Ingredients:\n");
                            for (com.google.gson.JsonElement ing : recipe.getAsJsonArray("ingredients")) {
                                com.google.gson.JsonObject ingObj = ing.getAsJsonObject();
                                sb.append("  - ")
                                    .append(
                                        ingObj.get("count")
                                            .getAsInt())
                                    .append("x ")
                                    .append(
                                        ingObj.get("name")
                                            .getAsString())
                                    .append("\n");
                            }
                        }
                        sb.append("Result: ");
                        if (recipe.has("resultCount")) {
                            sb.append(
                                recipe.get("resultCount")
                                    .getAsInt())
                                .append("x ");
                        }
                        sb.append(
                            recipe.has("result") ? recipe.get("result")
                                .getAsString() : entry.getName())
                            .append("\n");
                    } else {
                        sb.append("(Recipe data not available in readable format)\n");
                        sb.append(data);
                    }
                    break;

                case "note":
                    if (entry.getData()
                        .has("content")) {
                        sb.append(
                            entry.getData()
                                .get("content")
                                .getAsString());
                    } else {
                        sb.append("(Empty note)");
                    }
                    break;

                default:
                    sb.append(
                        entry.getData()
                            .toString());
            }

            return sb.toString();
        }

        @Override
        public String getDescription() {
            return "Query knowledge base entry or list all";
        }

        @Override
        public String getArgFormat() {
            return ":id_or_list";
        }
    }

    static class KbAddTool implements Tool {

        @Override
        public String execute(EntityPlayerMP player, String[] args) {
            if (args.length < 4) return "Error: need x, y, z, name";
            try {
                int x = Integer.parseInt(args[0]);
                int y = Integer.parseInt(args[1]);
                int z = Integer.parseInt(args[2]);
                StringBuilder name = new StringBuilder(args[3]);
                for (int i = 4; i < args.length; i++) {
                    name.append(":")
                        .append(args[i]);
                }

                World world = player.worldObj;
                Block block = world.getBlock(x, y, z);
                if (block.isAir(world, x, y, z)) {
                    return "Error: no block at [" + x + "," + y + "," + z + "]";
                }

                int dim = world.provider.dimensionId;
                if (KnowledgeBase.findByLocation(dim, x, y, z) != null) {
                    return "Already in KB at [" + x + "," + y + "," + z + "]";
                }

                GameRegistry.UniqueIdentifier uid = GameRegistry.findUniqueIdentifierFor(block);
                String blockId = uid != null ? uid.toString() : block.getUnlocalizedName();
                int meta = world.getBlockMetadata(x, y, z);

                KnowledgeEntry entry = new KnowledgeEntry(
                    KnowledgeBase.generateId(),
                    "block",
                    name.toString(),
                    KnowledgeEntry.blockData(dim, x, y, z, blockId, meta));
                KnowledgeBase.add(entry);
                return "Added to KB: " + name + " at [" + x + "," + y + "," + z + "]";
            } catch (NumberFormatException e) {
                return "Error: invalid coordinates";
            }
        }

        @Override
        public String getDescription() {
            return "Add block to knowledge base with custom name";
        }

        @Override
        public String getArgFormat() {
            return ":x:y:z:name";
        }
    }

    static class BlockTool implements Tool {

        @Override
        public String execute(EntityPlayerMP player, String[] args) {
            if (args.length < 3) return "Error: need x, y, z";
            try {
                int x = Integer.parseInt(args[0]);
                int y = Integer.parseInt(args[1]);
                int z = Integer.parseInt(args[2]);
                return WorldDataReader.getFullDataForAI(player.worldObj, x, y, z);
            } catch (NumberFormatException e) {
                return "Error: invalid coordinates";
            }
        }

        @Override
        public String getDescription() {
            return "Get block data at coords";
        }

        @Override
        public String getArgFormat() {
            return ":x:y:z";
        }
    }

    static class ItemSearchTool implements Tool {

        @Override
        public String execute(EntityPlayerMP player, String[] args) {
            if (args.length < 1) return "Error: need search query";
            StringBuilder query = new StringBuilder(args[0]);
            for (int i = 1; i < args.length; i++) {
                query.append(" ")
                    .append(args[i]);
            }
            String lowerQuery = query.toString()
                .toLowerCase();

            String[] words = lowerQuery.split("\\s+");
            StringBuilder patternBuilder = new StringBuilder();
            for (int i = 0; i < words.length; i++) {
                if (i > 0) patternBuilder.append(".*");
                patternBuilder.append(Pattern.quote(words[i]));
            }
            Pattern pattern = Pattern.compile(patternBuilder.toString(), Pattern.CASE_INSENSITIVE);

            List<String> results = new ArrayList<>();

            if (Loader.isModLoaded("NotEnoughItems")) {
                try {
                    for (ItemStack stack : codechicken.nei.ItemList.items) {
                        if (stack == null) continue;
                        String name = stack.getDisplayName();
                        if (name == null || name.contains("tile.") || name.contains("item.")) continue;
                        String itemId = Item.itemRegistry.getNameForObject(stack.getItem());
                        if (itemId == null) itemId = "unknown";

                        boolean nameMatch = pattern.matcher(name)
                            .find();
                        boolean idMatch = pattern.matcher(itemId)
                            .find();

                        if (nameMatch || idMatch) {
                            results.add(itemId + ":" + stack.getItemDamage() + " = " + name);
                            if (results.size() >= 25) break;
                        }
                    }
                } catch (Exception e) {
                    return "NEI search error: " + e.getMessage();
                }
            } else {
                for (Object obj : Item.itemRegistry) {
                    Item item = (Item) obj;
                    if (item == null) continue;
                    String itemId = Item.itemRegistry.getNameForObject(item);
                    if (itemId == null) continue;

                    try {
                        ItemStack stack = new ItemStack(item, 1, 0);
                        String displayName = stack.getDisplayName();
                        if (displayName != null && !displayName.contains("tile.") && !displayName.contains("item.")) {
                            if (pattern.matcher(displayName)
                                .find()
                                || pattern.matcher(itemId)
                                    .find()) {
                                results.add(itemId + ":0 = " + displayName);
                                if (results.size() >= 25) break;
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }

            if (results.isEmpty()) {
                return "No items found matching: " + query
                    + "\nTip: Try partial words like 'uran carb' for 'Uranium Carbide'";
            }

            StringBuilder sb = new StringBuilder("Found " + results.size() + " items:\n");
            for (String r : results) {
                sb.append("- ")
                    .append(r)
                    .append("\n");
            }
            if (results.size() >= 25) sb.append("(limited to 25 results)");
            return sb.toString();
        }

        @Override
        public String getDescription() {
            return "Search items by name (fuzzy word match, case-insensitive)";
        }

        @Override
        public String getArgFormat() {
            return ":query";
        }
    }

    static class RecipeTool implements Tool {

        private static final int DIRECT_DISPLAY_THRESHOLD = 8;
        private static final int SINGLE_HANDLER_MAX_DISPLAY = 20;

        @Override
        public String execute(EntityPlayerMP player, String[] args) {
            if (args.length < 1) return "Error: need item name. Usage: recipe:item or recipe:item:handler_filter";

            String itemQuery = args[0];
            String handlerFilter = args.length > 1 ? args[1] : null;

            List<KnowledgeEntry> kbRecipes = KnowledgeBase.getByType("recipe");
            for (KnowledgeEntry entry : kbRecipes) {
                String name = entry.getName()
                    .toLowerCase();
                if (name.contains(itemQuery.toLowerCase())) {
                    return formatKbRecipe(entry);
                }
            }

            try {
                ItemStack stack = cr.chromapie.knowsitall.nei.NEIRecipeQuery.findItemByName(itemQuery);
                if (stack == null) {
                    return "No item found matching: " + itemQuery;
                }

                var allRecipes = cr.chromapie.knowsitall.nei.NEIRecipeQuery.getRecipesFor(stack);
                if (allRecipes.isEmpty()) {
                    return "No recipes found for: " + stack.getDisplayName();
                }

                var byHandler = cr.chromapie.knowsitall.nei.NEIRecipeQuery.groupByHandler(allRecipes);
                int totalRecipes = allRecipes.size();
                int numHandlers = byHandler.size();

                if (handlerFilter != null && !handlerFilter.isEmpty()) {
                    return formatFilteredRecipes(stack, allRecipes, handlerFilter);
                }

                if (totalRecipes <= DIRECT_DISPLAY_THRESHOLD) {
                    return formatAllRecipes(stack, allRecipes, totalRecipes, numHandlers);
                }

                if (numHandlers == 1) {
                    return formatSingleHandler(stack, allRecipes, byHandler, totalRecipes);
                }

                return formatSummaryMode(stack, byHandler, totalRecipes, numHandlers);

            } catch (Exception e) {
                return "Error querying recipes: " + e.getMessage();
            }
        }

        private String formatKbRecipe(KnowledgeEntry entry) {
            StringBuilder sb = new StringBuilder();
            sb.append("[KB] ")
                .append(entry.getName())
                .append("\n");
            var data = entry.getData();
            if (data.has("recipe")) {
                var recipe = data.getAsJsonObject("recipe");
                if (recipe.has("handler")) {
                    sb.append("Handler: ")
                        .append(
                            recipe.get("handler")
                                .getAsString())
                        .append("\n");
                }
                if (recipe.has("ingredients")) {
                    sb.append("Ingredients: ");
                    var ings = recipe.getAsJsonArray("ingredients");
                    for (int i = 0; i < ings.size(); i++) {
                        var ing = ings.get(i)
                            .getAsJsonObject();
                        if (i > 0) sb.append(", ");
                        sb.append(
                            ing.get("count")
                                .getAsInt())
                            .append("x ")
                            .append(
                                ing.get("name")
                                    .getAsString());
                    }
                    sb.append("\n");
                }
                if (recipe.has("result")) {
                    sb.append("Result: ");
                    if (recipe.has("resultCount")) {
                        sb.append(
                            recipe.get("resultCount")
                                .getAsInt())
                            .append("x ");
                    }
                    sb.append(
                        recipe.get("result")
                            .getAsString())
                        .append("\n");
                }
            }
            return sb.toString();
        }

        private String formatAllRecipes(ItemStack stack,
            java.util.List<cr.chromapie.knowsitall.nei.NEIRecipeQuery.RecipeResult> recipes, int total, int handlers) {
            StringBuilder sb = new StringBuilder();
            sb.append("[NEI] ")
                .append(stack.getDisplayName())
                .append(" - ")
                .append(total)
                .append(" recipe(s) in ")
                .append(handlers)
                .append(" handler(s)\n\n");
            for (var r : recipes) {
                sb.append(r.toCompactString())
                    .append("\n");
            }
            return sb.toString();
        }

        private String formatSingleHandler(ItemStack stack,
            java.util.List<cr.chromapie.knowsitall.nei.NEIRecipeQuery.RecipeResult> recipes,
            java.util.Map<String, java.util.List<cr.chromapie.knowsitall.nei.NEIRecipeQuery.RecipeResult>> byHandler,
            int total) {
            StringBuilder sb = new StringBuilder();
            String handler = byHandler.keySet()
                .iterator()
                .next();
            sb.append("[NEI] ")
                .append(stack.getDisplayName())
                .append(" - ")
                .append(total)
                .append(" recipe(s) in ")
                .append(handler)
                .append("\n\n");
            int shown = 0;
            for (var r : recipes) {
                sb.append(r.toCompactString())
                    .append("\n");
                shown++;
                if (shown >= SINGLE_HANDLER_MAX_DISPLAY) {
                    sb.append("... and ")
                        .append(total - shown)
                        .append(" more recipes\n");
                    break;
                }
            }
            return sb.toString();
        }

        private String formatFilteredRecipes(ItemStack stack,
            java.util.List<cr.chromapie.knowsitall.nei.NEIRecipeQuery.RecipeResult> allRecipes, String handlerFilter) {
            var filtered = cr.chromapie.knowsitall.nei.NEIRecipeQuery.filterByHandler(allRecipes, handlerFilter);
            if (filtered.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                sb.append("No recipes for ")
                    .append(stack.getDisplayName())
                    .append(" in handler '")
                    .append(handlerFilter)
                    .append("'\n\nAvailable handlers: ");
                var byHandler = cr.chromapie.knowsitall.nei.NEIRecipeQuery.groupByHandler(allRecipes);
                boolean first = true;
                for (String h : byHandler.keySet()) {
                    if (!first) sb.append(", ");
                    sb.append(h);
                    first = false;
                }
                return sb.toString();
            }

            StringBuilder sb = new StringBuilder();
            sb.append("[NEI] ")
                .append(stack.getDisplayName())
                .append(" (")
                .append(handlerFilter)
                .append(") - ")
                .append(filtered.size())
                .append(" recipe(s)\n\n");
            int shown = 0;
            for (var r : filtered) {
                sb.append(r.toCompactString())
                    .append("\n");
                shown++;
                if (shown >= SINGLE_HANDLER_MAX_DISPLAY) {
                    sb.append("... and ")
                        .append(filtered.size() - shown)
                        .append(" more recipes\n");
                    break;
                }
            }
            return sb.toString();
        }

        private String formatSummaryMode(ItemStack stack,
            java.util.Map<String, java.util.List<cr.chromapie.knowsitall.nei.NEIRecipeQuery.RecipeResult>> byHandler,
            int total, int numHandlers) {
            StringBuilder sb = new StringBuilder();
            sb.append("[NEI] ")
                .append(stack.getDisplayName())
                .append(" - ")
                .append(total)
                .append(" recipe(s) in ")
                .append(numHandlers)
                .append(" handler(s)\n\n");

            sb.append("Available handlers:\n");
            for (var e : byHandler.entrySet()) {
                sb.append("  • ")
                    .append(e.getKey())
                    .append(": ")
                    .append(
                        e.getValue()
                            .size())
                    .append(" recipe(s)\n");
            }

            sb.append("\n[ACTION] Ask user which handler they want to see. ");
            sb.append("Then call recipe:")
                .append(stack.getDisplayName())
                .append(":handler_name");

            return sb.toString();
        }

        @Override
        public String getDescription() {
            return "Get recipe by name, optionally filter by handler";
        }

        @Override
        public String getArgFormat() {
            return ":item[:handler_filter]";
        }
    }

    static class KbNoteTool implements Tool {

        @Override
        public String execute(EntityPlayerMP player, String[] args) {
            if (args.length < 2) return "Error: need topic and note";
            String topic = args[0];
            StringBuilder note = new StringBuilder(args[1]);
            for (int i = 2; i < args.length; i++) {
                note.append(":")
                    .append(args[i]);
            }

            KnowledgeEntry existing = KnowledgeBase.findByName(topic);
            if (existing != null && "note".equals(existing.getType())) {
                KnowledgeBase.remove(existing.getId());
            }

            com.google.gson.JsonObject data = new com.google.gson.JsonObject();
            data.addProperty("content", note.toString());
            KnowledgeEntry entry = new KnowledgeEntry(KnowledgeBase.generateId(), "note", topic, data);
            KnowledgeBase.add(entry);
            return "Saved note: " + topic;
        }

        @Override
        public String getDescription() {
            return "Save a note/explanation to KB";
        }

        @Override
        public String getArgFormat() {
            return ":topic:content";
        }
    }
}
