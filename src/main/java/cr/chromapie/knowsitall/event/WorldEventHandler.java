package cr.chromapie.knowsitall.event;

import java.io.File;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.world.WorldEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cr.chromapie.knowsitall.CommonProxy;
import cr.chromapie.knowsitall.KnowsItAll;
import cr.chromapie.knowsitall.api.ConversationManager;
import cr.chromapie.knowsitall.knowledge.KnowledgeBase;

public class WorldEventHandler {

    private boolean initialized = false;

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        if (event.world.isRemote || initialized) {
            return;
        }

        MinecraftServer server = MinecraftServer.getServer();
        if (server == null) {
            return;
        }

        File worldDir = new File(CommonProxy.getMcDataDir(), "saves/" + server.getFolderName());
        if (server.isDedicatedServer()) {
            worldDir = new File(CommonProxy.getMcDataDir(), server.getFolderName());
        }

        KnowledgeBase.init(worldDir);
        ConversationManager.init(worldDir);
        initialized = true;
        KnowsItAll.LOG.info("Knowledge base initialized for world: {}", server.getFolderName());
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.world.isRemote) {
            return;
        }

        MinecraftServer server = MinecraftServer.getServer();
        if (server != null && server.worldServers != null) {
            boolean anyLoaded = false;
            for (var world : server.worldServers) {
                if (world != null && world != event.world) {
                    anyLoaded = true;
                    break;
                }
            }
            if (!anyLoaded) {
                KnowledgeBase.save();
                ConversationManager.save();
                initialized = false;
            }
        }
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        KnowledgeBase.save();
        ConversationManager.save();
    }
}
