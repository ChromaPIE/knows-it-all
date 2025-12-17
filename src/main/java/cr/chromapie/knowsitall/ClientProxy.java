package cr.chromapie.knowsitall;

import net.minecraftforge.common.MinecraftForge;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cr.chromapie.knowsitall.nei.CommandRecipe;
import cr.chromapie.knowsitall.nei.NEIIntegration;

public class ClientProxy extends CommonProxy {

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);

        FMLCommonHandler.instance()
            .bus()
            .register(new KeyHandler());

        if (Loader.isModLoaded("NotEnoughItems")) {
            MinecraftForge.EVENT_BUS.register(new NEIIntegration());
            KnowsItAll.LOG.info("NEI integration enabled");
        }
    }

    @Override
    public void serverStarting(FMLServerStartingEvent event) {
        super.serverStarting(event);

        if (Loader.isModLoaded("NotEnoughItems")) {
            event.registerServerCommand(new CommandRecipe());
        }
    }
}
