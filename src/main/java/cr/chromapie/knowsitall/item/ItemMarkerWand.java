package cr.chromapie.knowsitall.item;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

import cpw.mods.fml.common.registry.GameRegistry;
import cr.chromapie.knowsitall.ModCreativeTab;
import cr.chromapie.knowsitall.knowledge.KnowledgeBase;
import cr.chromapie.knowsitall.knowledge.KnowledgeEntry;
import cr.chromapie.knowsitall.util.ChatFormatter;

public class ItemMarkerWand extends Item {

    public ItemMarkerWand() {
        setUnlocalizedName("knowsitall.marker_wand");
        setTextureName("knowsitall:marker_wand");
        setMaxStackSize(1);
        setCreativeTab(ModCreativeTab.INSTANCE);
    }

    @Override
    public boolean onItemUseFirst(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ) {

        if (world.isRemote) {
            return true;
        }

        if (player.isSneaking()) {
            addToKnowledgeBase(player, world, x, y, z);
        } else {
            queryKnowledgeBase(player, world, x, y, z);
        }

        return true;
    }

    private void addToKnowledgeBase(EntityPlayer player, World world, int x, int y, int z) {
        int dim = world.provider.dimensionId;

        KnowledgeEntry existing = KnowledgeBase.findByLocation(dim, x, y, z);
        if (existing != null) {
            ChatFormatter.warning(player, "This block is already in Knowledge Base!");
            ChatFormatter.listItem(player, "ID", existing.getId());
            ChatFormatter.listItem(player, "Name", existing.getName());
            return;
        }

        Block block = world.getBlock(x, y, z);
        GameRegistry.UniqueIdentifier uid = GameRegistry.findUniqueIdentifierFor(block);
        String blockId = uid != null ? uid.toString() : block.getUnlocalizedName();
        int meta = world.getBlockMetadata(x, y, z);

        String defaultName = block.getLocalizedName() + " @ " + x + "," + y + "," + z;
        KnowledgeEntry entry = new KnowledgeEntry(
            KnowledgeBase.generateId(),
            "block",
            defaultName,
            KnowledgeEntry.blockData(dim, x, y, z, blockId, meta));
        KnowledgeBase.add(entry);

        ChatFormatter.success(player, "Added: " + block.getLocalizedName());
        ChatFormatter.listItem(player, "ID", entry.getId());
        ChatFormatter.info(player, "Tip: /knows kb rename " + entry.getId() + " <name>");
    }

    private void queryKnowledgeBase(EntityPlayer player, World world, int x, int y, int z) {
        KnowledgeEntry entry = KnowledgeBase.findByLocation(world.provider.dimensionId, x, y, z);

        if (entry == null) {
            Block block = world.getBlock(x, y, z);
            ChatFormatter.warning(player, block.getLocalizedName() + " is not in Knowledge Base.");
            ChatFormatter.info(player, "Sneak + Right-click to add it.");
            return;
        }

        Block currentBlock = world.getBlock(x, y, z);
        GameRegistry.UniqueIdentifier uid = GameRegistry.findUniqueIdentifierFor(currentBlock);
        String currentBlockId = uid != null ? uid.toString() : currentBlock.getUnlocalizedName();

        if (!entry.getBlockId()
            .equals(currentBlockId)) {
            ChatFormatter.warning(player, "Block mismatch detected!");
            ChatFormatter.listItem(player, "Recorded", entry.getBlockId());
            ChatFormatter.listItem(player, "Current", currentBlockId);
            ChatFormatter.info(player, "Use /knows kb remove " + entry.getId() + " to remove stale entry.");
            return;
        }

        ChatFormatter.header(player, entry.getName());
        ChatFormatter.listItem(player, "ID", entry.getId());
        ChatFormatter.listItem(player, "Block", entry.getBlockId());
        ChatFormatter.listItem(player, "Location", entry.getLocationString());
        ChatFormatter.info(player, "Use /knows query " + entry.getId() + " to ask AI.");
    }

    @Override
    @SuppressWarnings("unchecked")
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        tooltip.add(EnumChatFormatting.GRAY + "Right-click: Query block in KB");
        tooltip.add(EnumChatFormatting.GRAY + "Sneak + Right-click: Add to KB");
        tooltip.add("");
        tooltip.add(EnumChatFormatting.DARK_PURPLE + "Knowledge is power!");
    }
}
