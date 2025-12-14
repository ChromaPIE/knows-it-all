package cr.chromapie.knowsitall.util;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import cr.chromapie.knowsitall.network.ActionBarMessage;
import cr.chromapie.knowsitall.network.PacketHandler;

public final class ChatFormatter {

    private static final String PREFIX = EnumChatFormatting.DARK_PURPLE + "["
        + EnumChatFormatting.LIGHT_PURPLE
        + "KNOWS"
        + EnumChatFormatting.DARK_PURPLE
        + "] "
        + EnumChatFormatting.RESET;

    private static final String LINE = EnumChatFormatting.DARK_PURPLE + "─── "
        + EnumChatFormatting.LIGHT_PURPLE
        + "✦"
        + EnumChatFormatting.DARK_PURPLE
        + " ───";

    private ChatFormatter() {}

    public static void send(ICommandSender sender, String message) {
        sender.addChatMessage(new ChatComponentText(PREFIX + message));
    }

    public static void sendRaw(ICommandSender sender, String message) {
        sender.addChatMessage(new ChatComponentText(message));
    }

    public static void info(ICommandSender sender, String message) {
        send(sender, EnumChatFormatting.GRAY + message);
    }

    public static void success(ICommandSender sender, String message) {
        send(sender, EnumChatFormatting.GREEN + message);
    }

    public static void error(ICommandSender sender, String message) {
        send(sender, EnumChatFormatting.RED + message);
    }

    public static void warning(ICommandSender sender, String message) {
        send(sender, EnumChatFormatting.YELLOW + message);
    }

    public static void aiResponse(ICommandSender sender, String response) {
        String[] lines = response.split("\n");
        for (String line : lines) {
            if (line.trim()
                .isEmpty()) continue;
            sendRaw(sender, EnumChatFormatting.LIGHT_PURPLE + "✦ " + EnumChatFormatting.RESET + line);
        }
    }

    public static void userMessage(ICommandSender sender, String message) {
        sendRaw(sender, EnumChatFormatting.GRAY + "You: " + EnumChatFormatting.WHITE + message);
    }

    public static void thinking(ICommandSender sender) {
        if (sender instanceof EntityPlayerMP) {
            PacketHandler.INSTANCE.sendTo(
                new ActionBarMessage(EnumChatFormatting.LIGHT_PURPLE + "⏳ Thinking..."),
                (EntityPlayerMP) sender);
        }
    }

    public static void clearActionBar(ICommandSender sender) {
        if (sender instanceof EntityPlayerMP) {
            PacketHandler.INSTANCE.sendTo(new ActionBarMessage(""), (EntityPlayerMP) sender);
        }
    }

    public static void header(ICommandSender sender, String title) {
        sendRaw(sender, "");
        sendRaw(sender, LINE);
        sendRaw(sender, EnumChatFormatting.LIGHT_PURPLE + " " + title);
    }

    public static void listItem(ICommandSender sender, String key, String value) {
        sendRaw(
            sender,
            EnumChatFormatting.DARK_GRAY + "  • "
                + EnumChatFormatting.WHITE
                + key
                + EnumChatFormatting.GRAY
                + ": "
                + EnumChatFormatting.AQUA
                + value);
    }

    public static void commandHelp(ICommandSender sender, String command, String description) {
        sendRaw(sender, EnumChatFormatting.GOLD + "  " + command + EnumChatFormatting.GRAY + " - " + description);
    }
}
