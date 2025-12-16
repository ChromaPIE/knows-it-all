package cr.chromapie.knowsitall.network;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

import cr.chromapie.knowsitall.api.ChatMessage;
import cr.chromapie.knowsitall.ui.ChatScreen;
import cr.chromapie.knowsitall.util.ServerScheduler;

import java.util.ArrayList;
import java.util.List;

public class ChatSyncResponsePacket implements IMessage {

    private List<ChatMessage> messages;

    public ChatSyncResponsePacket() {
        this.messages = new ArrayList<>();
    }

    public ChatSyncResponsePacket(List<ChatMessage> messages) {
        this.messages = messages;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int size = buf.readInt();
        messages = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            String role = ByteBufUtils.readUTF8String(buf);
            String content = ByteBufUtils.readUTF8String(buf);
            messages.add(new ChatMessage(role, content));
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(messages.size());
        for (ChatMessage msg : messages) {
            ByteBufUtils.writeUTF8String(buf, msg.getRole());
            ByteBufUtils.writeUTF8String(buf, msg.getContent());
        }
    }

    public static class Handler implements IMessageHandler<ChatSyncResponsePacket, IMessage> {
        @Override
        public IMessage onMessage(ChatSyncResponsePacket packet, MessageContext ctx) {
            final List<ChatMessage> msgs = packet.messages;
            ServerScheduler.scheduleClient(() -> ChatScreen.syncMessages(msgs));
            return null;
        }
    }
}
