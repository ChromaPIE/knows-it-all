package cr.chromapie.knowsitall.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;

import cr.chromapie.knowsitall.api.ChatMessage;
import cr.chromapie.knowsitall.api.ConversationManager;
import cr.chromapie.knowsitall.util.ServerScheduler;

import java.util.List;

public class ChatSyncRequestPacket implements IMessage {

    public ChatSyncRequestPacket() {}

    @Override
    public void fromBytes(ByteBuf buf) {}

    @Override
    public void toBytes(ByteBuf buf) {}

    public static class Handler implements IMessageHandler<ChatSyncRequestPacket, IMessage> {
        @Override
        public IMessage onMessage(ChatSyncRequestPacket packet, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            ServerScheduler.schedule(() -> {
                List<ChatMessage> history = ConversationManager.getHistory(player.getUniqueID());
                PacketHandler.INSTANCE.sendTo(new ChatSyncResponsePacket(history), player);
            });
            return null;
        }
    }
}
