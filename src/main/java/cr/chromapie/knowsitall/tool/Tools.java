package cr.chromapie.knowsitall.tool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

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
                        .append(": ")
                        .append(e.getName())
                        .append("\n");
                }
                return sb.toString();
            }

            KnowledgeEntry entry = KnowledgeBase.get(args[0]);
            if (entry == null) entry = KnowledgeBase.findByName(args[0]);
            if (entry == null) return "Entry not found: " + args[0];

            return WorldDataReader.getFullDataForAI(player.worldObj, entry.getX(), entry.getY(), entry.getZ());
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
                String name = args[3];
                for (int i = 4; i < args.length; i++) {
                    name += ":" + args[i];
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
                    name,
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
            String query = args[0].toLowerCase();
            for (int i = 1; i < args.length; i++) {
                query += " " + args[i].toLowerCase();
            }

            List<String> results = new ArrayList<>();
            int count = 0;

            for (Object obj : Item.itemRegistry) {
                Item item = (Item) obj;
                if (item == null) continue;
                String itemId = Item.itemRegistry.getNameForObject(item);
                if (itemId == null) continue;

                // Quick check on item ID first
                boolean idMatch = itemId.toLowerCase()
                    .contains(query);

                // Only check meta 0 for most items
                try {
                    ItemStack stack = new ItemStack(item, 1, 0);
                    String displayName = stack.getDisplayName();
                    if (displayName != null && !displayName.contains("tile.") && !displayName.contains("item.")) {
                        if (idMatch || displayName.toLowerCase()
                            .contains(query)) {
                            results.add(itemId + ":0 = " + displayName);
                            count++;
                        }
                    }
                } catch (Exception ignored) {}

                if (count >= 20) break;
            }

            if (results.isEmpty()) {
                return "No items found matching: " + query;
            }

            StringBuilder sb = new StringBuilder("Found " + count + " items:\n");
            for (String r : results) {
                sb.append("- ")
                    .append(r)
                    .append("\n");
            }
            if (count >= 20) sb.append("(limited to 20 results)");
            return sb.toString();
        }

        @Override
        public String getDescription() {
            return "Search items by display name";
        }

        @Override
        public String getArgFormat() {
            return ":query";
        }
    }

    static class RecipeTool implements Tool {

        @Override
        public String execute(EntityPlayerMP player, String[] args) {
            if (args.length < 1) return "Error: need search query";
            String query = args[0].toLowerCase();
            for (int i = 1; i < args.length; i++) {
                query += " " + args[i].toLowerCase();
            }

            // First check knowledge base for marked recipes
            List<KnowledgeEntry> recipes = KnowledgeBase.getByType("recipe");
            StringBuilder found = new StringBuilder();
            int count = 0;

            for (KnowledgeEntry entry : recipes) {
                String name = entry.getName()
                    .toLowerCase();
                String dataStr = entry.getData()
                    .toString()
                    .toLowerCase();

                if (name.contains(query) || dataStr.contains(query)) {
                    if (count > 0) found.append("\n---\n");
                    found.append("[KB] Recipe: ")
                        .append(entry.getName())
                        .append("\n");

                    var data = entry.getData();
                    if (data.has("recipe")) {
                        var recipe = data.getAsJsonObject("recipe");
                        if (recipe.has("ingredients")) {
                            found.append("Ingredients: ");
                            var ings = recipe.getAsJsonArray("ingredients");
                            for (int i = 0; i < ings.size(); i++) {
                                var ing = ings.get(i)
                                    .getAsJsonObject();
                                if (i > 0) found.append(", ");
                                found.append(
                                    ing.get("count")
                                        .getAsInt())
                                    .append("x ")
                                    .append(
                                        ing.get("name")
                                            .getAsString());
                            }
                            found.append("\n");
                        }
                        if (recipe.has("result")) {
                            found.append("Result: ")
                                .append(
                                    recipe.get("resultCount")
                                        .getAsInt())
                                .append("x ")
                                .append(
                                    recipe.get("result")
                                        .getAsString())
                                .append("\n");
                        }
                        if (recipe.has("extra")) {
                            found.append("Extra: ")
                                .append(
                                    recipe.get("extra")
                                        .toString())
                                .append("\n");
                        }
                    }
                    count++;
                    if (count >= 3) break;
                }
            }

            // If not found in KB, query NEI directly
            if (count == 0) {
                try {
                    ItemStack stack = cr.chromapie.knowsitall.nei.NEIRecipeQuery.findItemByName(query);
                    if (stack != null) {
                        var neiRecipes = cr.chromapie.knowsitall.nei.NEIRecipeQuery.getRecipesFor(stack);
                        if (!neiRecipes.isEmpty()) {
                            found.append("[NEI] Found ")
                                .append(neiRecipes.size())
                                .append(" recipe(s) for ")
                                .append(stack.getDisplayName())
                                .append(":\n");
                            for (var r : neiRecipes) {
                                found.append("---\n")
                                    .append(r.toString())
                                    .append("\n");
                                count++;
                                if (count >= 3) break;
                            }
                        }
                    }
                } catch (Exception e) {
                    // NEI query failed, continue
                }
            }

            if (count == 0) {
                return "No recipe found for: " + query;
            }
            return found.toString();
        }

        @Override
        public String getDescription() {
            return "Get recipe by name (searches KB then NEI)";
        }

        @Override
        public String getArgFormat() {
            return ":query";
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
