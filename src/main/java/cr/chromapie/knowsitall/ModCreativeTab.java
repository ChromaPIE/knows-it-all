package cr.chromapie.knowsitall;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Items;
import net.minecraft.item.Item;

public class ModCreativeTab extends CreativeTabs {

    public static final ModCreativeTab INSTANCE = new ModCreativeTab();

    private ModCreativeTab() {
        super(KnowsItAll.MODID);
    }

    @Override
    public Item getTabIconItem() {
        return Items.experience_bottle;
    }

    @Override
    public String getTranslatedTabLabel() {
        return "Knows It All";
    }
}
