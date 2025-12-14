package cr.chromapie.knowsitall.item;

import net.minecraft.item.Item;

import cpw.mods.fml.common.registry.GameRegistry;

public final class ModItems {

    public static Item markerWand;

    private ModItems() {}

    public static void init() {
        markerWand = new ItemMarkerWand();
        GameRegistry.registerItem(markerWand, "marker_wand");
    }
}
