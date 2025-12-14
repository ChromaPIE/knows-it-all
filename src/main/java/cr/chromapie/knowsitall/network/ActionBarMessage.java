package cr.chromapie.knowsitall.network;

import net.minecraft.client.Minecraft;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class ActionBarMessage implements IMessage {

    private String message;

    public ActionBarMessage() {}

    public ActionBarMessage(String message) {
        this.message = message;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        message = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, message);
    }

    public static class Handler implements IMessageHandler<ActionBarMessage, IMessage> {

        @Override
        public IMessage onMessage(ActionBarMessage msg, MessageContext ctx) {
            Minecraft.getMinecraft().ingameGUI.func_110326_a(msg.message, false);
            return null;
        }
    }
}
