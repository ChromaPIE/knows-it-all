package cr.chromapie.knowsitall;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;

@Mod(
    modid = KnowsItAll.MODID,
    version = Tags.VERSION,
    name = KnowsItAll.MODNAME,
    acceptedMinecraftVersions = "[1.7.10]")
public class KnowsItAll {

    public static final String MODID = "knowsitall";
    public static final String MODNAME = "Knows It All";

    public static final Logger LOG = LogManager.getLogger(MODID);

    @SidedProxy(clientSide = "cr.chromapie.knowsitall.ClientProxy", serverSide = "cr.chromapie.knowsitall.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        proxy.serverStarting(event);
    }

    @Mod.EventHandler
    public void serverStopping(FMLServerStoppingEvent event) {
        proxy.serverStopping(event);
    }
}
