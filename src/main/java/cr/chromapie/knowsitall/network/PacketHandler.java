package cr.chromapie.knowsitall.network;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;

public class PacketHandler {

    public static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel("knowsitall");

    public static void init() {
        INSTANCE.registerMessage(ChatRequestPacket.Handler.class, ChatRequestPacket.class, 1, Side.SERVER);
        INSTANCE.registerMessage(ChatResponsePacket.Handler.class, ChatResponsePacket.class, 2, Side.CLIENT);
        INSTANCE.registerMessage(ChatSyncRequestPacket.Handler.class, ChatSyncRequestPacket.class, 3, Side.SERVER);
        INSTANCE.registerMessage(ChatSyncResponsePacket.Handler.class, ChatSyncResponsePacket.class, 4, Side.CLIENT);
    }
}
