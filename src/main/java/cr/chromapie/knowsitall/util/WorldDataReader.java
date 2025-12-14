package cr.chromapie.knowsitall.util;

import net.minecraft.block.Block;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import cpw.mods.fml.common.registry.GameRegistry;

public final class WorldDataReader {

    private WorldDataReader() {}

    public static String getBlockInfo(World world, int x, int y, int z) {
        Block block = world.getBlock(x, y, z);
        int meta = world.getBlockMetadata(x, y, z);

        GameRegistry.UniqueIdentifier uid = GameRegistry.findUniqueIdentifierFor(block);
        String blockId = uid != null ? uid.toString() : block.getUnlocalizedName();

        StringBuilder sb = new StringBuilder();
        sb.append("Block: ")
            .append(blockId);
        sb.append("\nMeta: ")
            .append(meta);
        sb.append("\nDisplay Name: ")
            .append(block.getLocalizedName());

        return sb.toString();
    }

    public static String getTileEntityData(World world, int x, int y, int z) {
        TileEntity te = world.getTileEntity(x, y, z);
        if (te == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("TileEntity: ")
            .append(
                te.getClass()
                    .getSimpleName());

        if (te instanceof IInventory) {
            sb.append("\n\n")
                .append(getInventoryContents((IInventory) te));
        }

        NBTTagCompound nbt = new NBTTagCompound();
        te.writeToNBT(nbt);
        sb.append("\n\nNBT Data (summary):\n")
            .append(summarizeNBT(nbt, 0, 3));

        return sb.toString();
    }

    public static String getInventoryContents(IInventory inv) {
        StringBuilder sb = new StringBuilder();
        sb.append("Inventory: ")
            .append(inv.getInventoryName());
        sb.append(" (")
            .append(inv.getSizeInventory())
            .append(" slots)\n");

        int itemCount = 0;
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack != null) {
                itemCount++;
                sb.append(String.format("  Slot %d: %s x%d\n", i, stack.getDisplayName(), stack.stackSize));
            }
        }

        if (itemCount == 0) {
            sb.append("  (empty)");
        } else {
            sb.append(String.format("Total: %d item types", itemCount));
        }

        return sb.toString();
    }

    public static String getFullDataForAI(World world, int x, int y, int z) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Location: [")
            .append(x)
            .append(", ")
            .append(y)
            .append(", ")
            .append(z)
            .append("] ===\n");
        sb.append("Dimension: ")
            .append(world.provider.dimensionId)
            .append(" (")
            .append(world.provider.getDimensionName())
            .append(")\n\n");

        sb.append(getBlockInfo(world, x, y, z));

        String teData = getTileEntityData(world, x, y, z);
        if (teData != null) {
            sb.append("\n\n")
                .append(teData);
        }

        return sb.toString();
    }

    private static String summarizeNBT(NBTTagCompound nbt, int depth, int maxDepth) {
        if (depth >= maxDepth) {
            return "(...)";
        }

        StringBuilder sb = new StringBuilder();
        StringBuilder indentBuilder = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            indentBuilder.append("  ");
        }
        String indent = indentBuilder.toString();

        for (String keyObj : nbt.func_150296_c()) {
            byte type = nbt.getTag(keyObj)
                .getId();

            sb.append(indent)
                .append(keyObj)
                .append(": ");

            switch (type) {
                case 1: // Byte
                    sb.append(nbt.getByte(keyObj));
                    break;
                case 2: // Short
                    sb.append(nbt.getShort(keyObj));
                    break;
                case 3: // Int
                    sb.append(nbt.getInteger(keyObj));
                    break;
                case 4: // Long
                    sb.append(nbt.getLong(keyObj));
                    break;
                case 5: // Float
                    sb.append(nbt.getFloat(keyObj));
                    break;
                case 6: // Double
                    sb.append(nbt.getDouble(keyObj));
                    break;
                case 8: // String
                    String strVal = nbt.getString(keyObj);
                    if (strVal.length() > 50) {
                        strVal = strVal.substring(0, 47) + "...";
                    }
                    sb.append("\"")
                        .append(strVal)
                        .append("\"");
                    break;
                case 9: // List
                    sb.append("[list]");
                    break;
                case 10: // Compound
                    sb.append("{\n")
                        .append(summarizeNBT(nbt.getCompoundTag(keyObj), depth + 1, maxDepth))
                        .append(indent)
                        .append("}");
                    break;
                default:
                    sb.append("[")
                        .append(type)
                        .append("]");
            }
            sb.append("\n");
        }

        return sb.toString();
    }
}
