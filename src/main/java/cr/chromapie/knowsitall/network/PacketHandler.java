package cr.chromapie.knowsitall.network;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;

public class PacketHandler {

    public static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel("knowsitall");

    public static void init() {
        INSTANCE.registerMessage(ActionBarMessage.Handler.class, ActionBarMessage.class, 0, Side.CLIENT);
    }
}
