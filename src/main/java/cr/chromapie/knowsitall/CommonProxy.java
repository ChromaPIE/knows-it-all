package cr.chromapie.knowsitall;

import java.io.File;

import net.minecraftforge.common.MinecraftForge;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;
import cr.chromapie.knowsitall.api.OpenAIClient;
import cr.chromapie.knowsitall.command.CommandKnows;
import cr.chromapie.knowsitall.event.WorldEventHandler;
import cr.chromapie.knowsitall.item.ModItems;
import cr.chromapie.knowsitall.network.PacketHandler;
import cr.chromapie.knowsitall.tool.Tools;
import cr.chromapie.knowsitall.util.ServerScheduler;

public class CommonProxy {

    private static File mcDataDir;

    public void preInit(FMLPreInitializationEvent event) {
        mcDataDir = event.getModConfigurationDirectory()
            .getParentFile();
        ModConfig.init(event.getSuggestedConfigurationFile());
        PacketHandler.init();
        ModItems.init();
        Tools.registerAll();
        KnowsItAll.LOG.info("Knows It All is loading...");
    }

    public void init(FMLInitializationEvent event) {
        WorldEventHandler handler = new WorldEventHandler();
        MinecraftForge.EVENT_BUS.register(handler);
        FMLCommonHandler.instance()
            .bus()
            .register(handler);
        FMLCommonHandler.instance()
            .bus()
            .register(new ServerScheduler());
    }

    public void postInit(FMLPostInitializationEvent event) {}

    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandKnows());
    }

    public void serverStopping(FMLServerStoppingEvent event) {
        OpenAIClient.shutdown();
    }

    public static File getMcDataDir() {
        return mcDataDir;
    }
}
