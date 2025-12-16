package cr.chromapie.knowsitall.network;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;

import cr.chromapie.knowsitall.ModConfig;
import cr.chromapie.knowsitall.api.ConversationManager;
import cr.chromapie.knowsitall.api.OpenAIClient;
import cr.chromapie.knowsitall.context.ContextCollector;
import cr.chromapie.knowsitall.tool.ToolRegistry;
import cr.chromapie.knowsitall.util.ServerScheduler;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ChatRequestPacket implements IMessage {

    private String message;

    public ChatRequestPacket() {}

    public ChatRequestPacket(String message) {
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

    public static class Handler implements IMessageHandler<ChatRequestPacket, IMessage> {
        private static final int MAX_TOOL_ITERATIONS = 5;

        @Override
        public IMessage onMessage(ChatRequestPacket packet, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            ServerScheduler.schedule(() -> handleRequest(player, packet.message));
            return null;
        }

        private void handleRequest(EntityPlayerMP player, String message) {
            if (!ModConfig.isConfigured()) {
                sendResponse(player, "API not configured. Use /knows config key <your-api-key>", ChatResponsePacket.TYPE_ERROR);
                return;
            }

            String context = ContextCollector.buildContextString(player, true, true, ModConfig.getDefaultScanRange());

            OpenAIClient.chat(
                player.getUniqueID(),
                message,
                context,
                response -> processResponse(player, response, 0, new HashSet<>()),
                error -> sendResponse(player, "Error: " + error, ChatResponsePacket.TYPE_ERROR)
            );
        }

        private String getToolSignature(ToolRegistry.ToolCall call) {
            return call.name + ":" + String.join(":", call.args);
        }

        private void processResponse(EntityPlayerMP player, String response, int iteration, Set<String> executedTools) {
            List<ToolRegistry.ToolCall> toolCalls = ToolRegistry.parseToolCalls(response);
            toolCalls.removeIf(call -> executedTools.contains(getToolSignature(call)));

            if (!toolCalls.isEmpty() && iteration < MAX_TOOL_ITERATIONS) {
                ServerScheduler.schedule(() -> {
                    String cleanResponse = ToolRegistry.cleanResponse(response);
                    if (ToolRegistry.isMeaningfulResponse(cleanResponse)) {
                        sendResponse(player, cleanResponse, ChatResponsePacket.TYPE_NORMAL);
                    }

                    StringBuilder toolHint = new StringBuilder();
                    for (ToolRegistry.ToolCall call : toolCalls) {
                        if (toolHint.length() > 0) toolHint.append(", ");
                        toolHint.append(call.name);
                        if (call.args.length > 0) {
                            toolHint.append("(").append(String.join(", ", call.args)).append(")");
                        }
                    }
                    String hintText = "Using tools: " + toolHint.toString() + "...";
                    ConversationManager.addToolHint(player.getUniqueID(), hintText);
                    sendResponse(player, hintText, ChatResponsePacket.TYPE_TOOL_HINT);

                    for (ToolRegistry.ToolCall call : toolCalls) {
                        executedTools.add(getToolSignature(call));
                    }

                    String toolResults = ToolRegistry.executeTools(player, toolCalls);

                    OpenAIClient.continueWithToolResult(
                        player.getUniqueID(),
                        toolResults,
                        followUp -> processResponse(player, followUp, iteration + 1, executedTools),
                        err -> sendResponse(player, "Error: " + err, ChatResponsePacket.TYPE_ERROR)
                    );
                });
            } else {
                String cleanResponse = ToolRegistry.cleanResponse(response);
                if (ToolRegistry.isMeaningfulResponse(cleanResponse)) {
                    sendResponse(player, cleanResponse, ChatResponsePacket.TYPE_NORMAL);
                }
            }
        }

        private void sendResponse(EntityPlayerMP player, String content, int messageType) {
            ServerScheduler.schedule(() -> {
                PacketHandler.INSTANCE.sendTo(new ChatResponsePacket(content, messageType), player);
            });
        }
    }
}
