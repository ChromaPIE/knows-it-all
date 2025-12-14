package cr.chromapie.knowsitall.context;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;

import cpw.mods.fml.common.registry.GameRegistry;

public class BlockScanner {

    private static final Pattern SCAN_PATTERN = Pattern.compile("\\[SCAN:([^:]+):(\\d+)]");
    private static final int MAX_RANGE = 16;

    public static class ScanRequest {

        public final String blockFilter;
        public final int range;

        public ScanRequest(String blockFilter, int range) {
            this.blockFilter = blockFilter;
            this.range = Math.min(range, MAX_RANGE);
        }
    }

    public static ScanRequest parseScanRequest(String response) {
        Matcher m = SCAN_PATTERN.matcher(response);
        if (m.find()) {
            String filter = m.group(1)
                .toLowerCase();
            int range = Integer.parseInt(m.group(2));
            return new ScanRequest(filter, range);
        }
        return null;
    }

    public static String scan(EntityPlayerMP player, ScanRequest request) {
        World world = player.worldObj;
        int px = (int) Math.floor(player.posX);
        int py = (int) Math.floor(player.posY);
        int pz = (int) Math.floor(player.posZ);

        Map<String, Integer> counts = new HashMap<>();
        Map<String, String> firstPos = new HashMap<>();

        int rangeSq = request.range * request.range;
        for (int x = px - request.range; x <= px + request.range; x++) {
            for (int y = py - request.range; y <= py + request.range; y++) {
                for (int z = pz - request.range; z <= pz + request.range; z++) {
                    int dx = x - px, dy = y - py, dz = z - pz;
                    if (dx * dx + dy * dy + dz * dz > rangeSq) continue;

                    Block block = world.getBlock(x, y, z);
                    if (block.isAir(world, x, y, z)) continue;

                    String name = block.getLocalizedName()
                        .toLowerCase();
                    GameRegistry.UniqueIdentifier uid = GameRegistry.findUniqueIdentifierFor(block);
                    String id = uid != null ? uid.toString()
                        .toLowerCase() : "";

                    if (matchesFilter(name, id, request.blockFilter)) {
                        String key = block.getLocalizedName();
                        counts.merge(key, 1, Integer::sum);
                        if (!firstPos.containsKey(key)) {
                            firstPos.put(key, "[" + x + ", " + y + ", " + z + "]");
                        }
                    }
                }
            }
        }

        if (counts.isEmpty()) {
            return "=== Scan Result ===\nNo blocks matching '" + request.blockFilter
                + "' found within "
                + request.range
                + " blocks.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== Scan Result for '")
            .append(request.blockFilter)
            .append("' (range ")
            .append(request.range)
            .append(") ===\n");

        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            sb.append(entry.getKey())
                .append(": ")
                .append(entry.getValue())
                .append(" (nearest at ")
                .append(firstPos.get(entry.getKey()))
                .append(")\n");
        }

        return sb.toString();
    }

    private static boolean matchesFilter(String name, String id, String filter) {
        return name.contains(filter) || id.contains(filter);
    }
}
