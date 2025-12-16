package cr.chromapie.knowsitall.network;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cr.chromapie.knowsitall.ui.ChatScreen;
import cr.chromapie.knowsitall.util.ServerScheduler;
import io.netty.buffer.ByteBuf;

public class ChatResponsePacket implements IMessage {

    public static final int TYPE_NORMAL = 0;
    public static final int TYPE_ERROR = 1;
    public static final int TYPE_TOOL_HINT = 2;

    private String content;
    private int messageType;

    public ChatResponsePacket() {}

    public ChatResponsePacket(String content, boolean isError) {
        this.content = content;
        this.messageType = isError ? TYPE_ERROR : TYPE_NORMAL;
    }

    public ChatResponsePacket(String content, int messageType) {
        this.content = content;
        this.messageType = messageType;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        content = ByteBufUtils.readUTF8String(buf);
        messageType = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, content);
        buf.writeInt(messageType);
    }

    public static class Handler implements IMessageHandler<ChatResponsePacket, IMessage> {

        @Override
        public IMessage onMessage(ChatResponsePacket packet, MessageContext ctx) {
            final String content = packet.content;
            final int type = packet.messageType;
            ServerScheduler.scheduleClient(() -> ChatScreen.receiveResponse(content, type));
            return null;
        }
    }
}
