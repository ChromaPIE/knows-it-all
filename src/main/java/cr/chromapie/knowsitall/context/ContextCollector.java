package cr.chromapie.knowsitall.context;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import cpw.mods.fml.common.registry.GameRegistry;
import cr.chromapie.knowsitall.util.WorldDataReader;

public class ContextCollector {

    public record BlockContext(int x, int y, int z, String blockId, String displayName, String data) {}

    public static BlockContext getLookingAt(EntityPlayerMP player, double maxDistance) {
        Vec3 eyePos = Vec3.createVectorHelper(player.posX, player.posY + player.getEyeHeight(), player.posZ);

        Vec3 lookVec = player.getLookVec();
        Vec3 targetPos = Vec3.createVectorHelper(
            eyePos.xCoord + lookVec.xCoord * maxDistance,
            eyePos.yCoord + lookVec.yCoord * maxDistance,
            eyePos.zCoord + lookVec.zCoord * maxDistance);

        MovingObjectPosition hit = player.worldObj.rayTraceBlocks(eyePos, targetPos);

        if (hit == null || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            return null;
        }

        return getBlockContext(player.worldObj, hit.blockX, hit.blockY, hit.blockZ);
    }

    public static BlockContext getBlockContext(World world, int x, int y, int z) {
        Block block = world.getBlock(x, y, z);
        if (block.isAir(world, x, y, z)) {
            return null;
        }

        GameRegistry.UniqueIdentifier uid = GameRegistry.findUniqueIdentifierFor(block);
        String blockId = uid != null ? uid.toString() : block.getUnlocalizedName();
        String displayName = block.getLocalizedName();
        String data = WorldDataReader.getFullDataForAI(world, x, y, z);

        return new BlockContext(x, y, z, blockId, displayName, data);
    }

    public static List<BlockContext> getContainersInRange(EntityPlayerMP player, int range) {
        List<BlockContext> containers = new ArrayList<>();
        World world = player.worldObj;

        int px = (int) Math.floor(player.posX);
        int py = (int) Math.floor(player.posY);
        int pz = (int) Math.floor(player.posZ);

        for (int x = px - range; x <= px + range; x++) {
            for (int y = py - range; y <= py + range; y++) {
                for (int z = pz - range; z <= pz + range; z++) {
                    TileEntity te = world.getTileEntity(x, y, z);
                    if (te instanceof IInventory) {
                        BlockContext ctx = getBlockContext(world, x, y, z);
                        if (ctx != null) {
                            containers.add(ctx);
                        }
                    }
                }
            }
        }

        return containers;
    }

    public static String getPlayerInventory(EntityPlayerMP player) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Player Inventory ===\n");
        sb.append(WorldDataReader.getInventoryContents(player.inventory));
        return sb.toString();
    }

    public static String buildContextString(EntityPlayerMP player, boolean includeLookingAt, boolean includeInventory,
        int containerRange) {
        StringBuilder sb = new StringBuilder();

        sb.append("=== Player Context ===\n");
        sb.append("Position: [")
            .append((int) player.posX)
            .append(", ")
            .append((int) player.posY)
            .append(", ")
            .append((int) player.posZ)
            .append("]\n");
        sb.append("Dimension: ")
            .append(player.worldObj.provider.getDimensionName())
            .append("\n");

        if (player.getHeldItem() != null) {
            sb.append("Held Item (main hand): ")
                .append(
                    player.getHeldItem()
                        .getDisplayName());
            if (player.getHeldItem().stackSize > 1) {
                sb.append(" x")
                    .append(player.getHeldItem().stackSize);
            }
            sb.append("\n");
        } else {
            sb.append("Held Item: (empty hand)\n");
        }
        sb.append("\n");

        if (includeLookingAt) {
            BlockContext lookAt = getLookingAt(player, 5.0);
            if (lookAt != null) {
                sb.append("=== Block Player Is Looking At ===\n");
                sb.append(lookAt.data)
                    .append("\n\n");
            } else {
                sb.append("=== Looking At: Nothing (air or too far) ===\n\n");
            }
        }

        if (containerRange > 0) {
            List<BlockContext> containers = getContainersInRange(player, containerRange);
            if (!containers.isEmpty()) {
                sb.append("=== Containers Within ")
                    .append(containerRange)
                    .append(" Blocks ===\n");
                for (BlockContext ctx : containers) {
                    sb.append("\n--- ")
                        .append(ctx.displayName)
                        .append(" at [")
                        .append(ctx.x)
                        .append(", ")
                        .append(ctx.y)
                        .append(", ")
                        .append(ctx.z)
                        .append("] ---\n");
                    sb.append(ctx.data)
                        .append("\n");
                }
            } else {
                sb.append("=== No containers within ")
                    .append(containerRange)
                    .append(" blocks ===\n\n");
            }
        }

        if (includeInventory) {
            sb.append("\n")
                .append(getPlayerInventory(player));
        }

        return sb.toString();
    }
}
