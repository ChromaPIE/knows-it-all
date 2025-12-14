package cr.chromapie.knowsitall.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

import cpw.mods.fml.common.registry.GameRegistry;
import cr.chromapie.knowsitall.ModConfig;
import cr.chromapie.knowsitall.api.ConversationManager;
import cr.chromapie.knowsitall.api.OpenAIClient;
import cr.chromapie.knowsitall.context.ContextCollector;
import cr.chromapie.knowsitall.knowledge.KnowledgeBase;
import cr.chromapie.knowsitall.knowledge.KnowledgeEntry;
import cr.chromapie.knowsitall.tool.ToolRegistry;
import cr.chromapie.knowsitall.util.ChatFormatter;
import cr.chromapie.knowsitall.util.ServerScheduler;
import cr.chromapie.knowsitall.util.WorldDataReader;

public class CommandKnows extends CommandBase {

    private static final String[] SUBCOMMANDS = { "ask", "config", "kb", "query", "clear", "help" };
    private static final String[] CONFIG_OPTIONS = { "url", "key", "model", "reload" };
    private static final String[] KB_OPTIONS = { "list", "rename", "remove", "clear", "info" };

    @Override
    public String getCommandName() {
        return "knows";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/knows <question> OR /knows <ask|config|kb|query|help> [args...]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 0) {
            showHelp(sender);
            return;
        }

        String subCommand = args[0].toLowerCase();
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);

        switch (subCommand) {
            case "config":
            case "cfg":
                handleConfig(sender, subArgs);
                break;
            case "kb":
            case "knowledge":
                handleKnowledgeBase(sender, subArgs);
                break;
            case "query":
            case "q":
                handleQuery(sender, subArgs, false);
                break;
            case "queryforce":
            case "qf":
                handleQuery(sender, subArgs, true);
                break;
            case "clear":
                handleClear(sender);
                break;
            case "help":
            case "?":
                showHelp(sender);
                break;
            case "ask":
                handleAsk(sender, subArgs);
                break;
            default:
                handleAsk(sender, args);
        }
    }

    private void handleAsk(ICommandSender sender, String[] args) {
        if (args.length == 0) {
            ChatFormatter.error(sender, "Usage: /knows <your question>");
            return;
        }

        if (!ModConfig.isConfigured()) {
            ChatFormatter.error(sender, "API not configured!");
            ChatFormatter.info(sender, "Use /knows config key <your-api-key>");
            return;
        }

        if (!(sender instanceof EntityPlayerMP)) {
            ChatFormatter.error(sender, "This command requires a player.");
            return;
        }

        EntityPlayerMP player = (EntityPlayerMP) sender;
        String question = String.join(" ", args);

        String context = ContextCollector.buildContextString(player, true, true, ModConfig.getDefaultScanRange());

        ChatFormatter.userMessage(sender, question);
        ChatFormatter.thinking(sender);

        final EntityPlayerMP finalPlayer = player;

        OpenAIClient.chat(
            player.getUniqueID(),
            question,
            context,
            response -> { processResponse(sender, finalPlayer, response, 0); },
            error -> {
                ServerScheduler.schedule(() -> {
                    ChatFormatter.clearActionBar(sender);
                    ChatFormatter.error(sender, error);
                });
            });
    }

    private static final int MAX_TOOL_ITERATIONS = 5;

    private void processResponse(ICommandSender sender, EntityPlayerMP player, String response, int iteration) {
        List<ToolRegistry.ToolCall> toolCalls = ToolRegistry.parseToolCalls(response);
        if (!toolCalls.isEmpty() && iteration < MAX_TOOL_ITERATIONS) {
            ServerScheduler.schedule(() -> {
                String cleanResponse = ToolRegistry.cleanResponse(response);
                if (!cleanResponse.isEmpty()) {
                    ChatFormatter.clearActionBar(sender);
                    ChatFormatter.aiResponse(sender, cleanResponse);
                }
                String toolInfo = toolCalls.stream()
                    .map(tc -> tc.name + (tc.args.length > 0 ? ":" + tc.args[0] : ""))
                    .collect(java.util.stream.Collectors.joining(", "));
                ChatFormatter.sendRaw(
                    sender,
                    EnumChatFormatting.DARK_GRAY + "  ⚙ "
                        + toolInfo
                        + (iteration > 0 ? "§7 (" + (iteration + 1) + ")" : ""));
                ChatFormatter.thinking(sender);
                String toolResults = ToolRegistry.executeTools(player, toolCalls);

                OpenAIClient.continueWithToolResult(
                    player.getUniqueID(),
                    toolResults,
                    followUp -> { processResponse(sender, player, followUp, iteration + 1); },
                    err -> {
                        ServerScheduler.schedule(() -> {
                            ChatFormatter.clearActionBar(sender);
                            ChatFormatter.error(sender, err);
                        });
                    });
            });
        } else {
            ServerScheduler.schedule(() -> {
                ChatFormatter.clearActionBar(sender);
                ChatFormatter.aiResponse(sender, response);
            });
        }
    }

    private void handleClear(ICommandSender sender) {
        if (!(sender instanceof EntityPlayerMP)) {
            ChatFormatter.error(sender, "Requires player.");
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) sender;
        int count = ConversationManager.getMessageCount(player.getUniqueID());
        ConversationManager.clear(player.getUniqueID());
        ChatFormatter.success(sender, "Cleared " + count + " messages from history.");
    }

    private void handleConfig(ICommandSender sender, String[] args) {
        if (args.length == 0) {
            showCurrentConfig(sender);
            return;
        }

        String option = args[0].toLowerCase();
        String[] valueArgs = Arrays.copyOfRange(args, 1, args.length);
        String value = String.join(" ", valueArgs);

        switch (option) {
            case "url":
                if (value.isEmpty()) {
                    ChatFormatter.listItem(sender, "API URL", ModConfig.getApiUrl());
                } else {
                    ModConfig.setApiUrl(value);
                    ChatFormatter.success(sender, "API URL updated.");
                }
                break;
            case "key":
                if (value.isEmpty()) {
                    String key = ModConfig.getApiKey();
                    String masked = key.isEmpty() ? "(not set)"
                        : key.substring(0, Math.min(8, key.length())) + "..."
                            + key.substring(Math.max(0, key.length() - 4));
                    ChatFormatter.listItem(sender, "API Key", masked);
                } else {
                    ModConfig.setApiKey(value);
                    ChatFormatter.success(sender, "API Key updated.");
                }
                break;
            case "model":
                if (value.isEmpty()) {
                    ChatFormatter.listItem(sender, "Model", ModConfig.getModel());
                } else {
                    ModConfig.setModel(value);
                    ChatFormatter.success(sender, "Model updated to: " + value);
                }
                break;
            case "reload":
                ModConfig.reload();
                ChatFormatter.success(sender, "Config reloaded from file.");
                break;
            default:
                ChatFormatter.error(sender, "Unknown option: " + option);
                ChatFormatter.info(sender, "Available: url, key, model, reload");
        }
    }

    private void handleKnowledgeBase(ICommandSender sender, String[] args) {
        if (args.length == 0) {
            handleKbList(sender);
            return;
        }

        String action = args[0].toLowerCase();
        String[] actionArgs = Arrays.copyOfRange(args, 1, args.length);

        switch (action) {
            case "list":
            case "ls":
                handleKbList(sender);
                break;
            case "info":
            case "i":
                handleKbInfo(sender, actionArgs);
                break;
            case "rename":
            case "rn":
                handleKbRename(sender, actionArgs);
                break;
            case "remove":
            case "rm":
            case "delete":
                handleKbRemove(sender, actionArgs);
                break;
            case "clear":
                handleKbClear(sender);
                break;
            default:
                ChatFormatter.error(sender, "Unknown action: " + action);
                ChatFormatter.info(sender, "Available: list, info, rename, remove, clear");
        }
    }

    private void handleKbList(ICommandSender sender) {
        List<KnowledgeEntry> entries = KnowledgeBase.getAll();

        if (entries.isEmpty()) {
            ChatFormatter.header(sender, "Knowledge Base");
            ChatFormatter.info(sender, "Empty. Use Marker Wand to bookmark blocks.");
            return;
        }

        ChatFormatter.header(sender, "Knowledge Base (" + entries.size() + ")");
        for (KnowledgeEntry entry : entries) {
            ChatFormatter.listItem(sender, entry.getId(), entry.getName());
        }
    }

    private void handleKbInfo(ICommandSender sender, String[] args) {
        if (args.length == 0) {
            ChatFormatter.error(sender, "Usage: /knows kb info <id>");
            return;
        }

        String query = String.join(" ", args);
        KnowledgeEntry entry = KnowledgeBase.get(query);
        if (entry == null) entry = KnowledgeBase.findByName(query);

        if (entry == null) {
            ChatFormatter.error(sender, "Not found: " + query);
            return;
        }

        ChatFormatter.header(sender, entry.getName());
        ChatFormatter.listItem(sender, "ID", entry.getId());
        ChatFormatter.listItem(sender, "Block", entry.getBlockId());
        ChatFormatter.listItem(sender, "Location", entry.getLocationString());
    }

    private void handleKbRename(ICommandSender sender, String[] args) {
        if (args.length < 2) {
            ChatFormatter.error(sender, "Usage: /knows kb rename <id> <name>");
            return;
        }

        String id = args[0];
        String newName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        if (KnowledgeBase.rename(id, newName)) {
            ChatFormatter.success(sender, "Renamed to: " + newName);
        } else {
            ChatFormatter.error(sender, "Not found: " + id);
        }
    }

    private void handleKbRemove(ICommandSender sender, String[] args) {
        if (args.length == 0) {
            ChatFormatter.error(sender, "Usage: /knows kb remove <id>");
            return;
        }

        if (KnowledgeBase.remove(args[0])) {
            ChatFormatter.success(sender, "Removed: " + args[0]);
        } else {
            ChatFormatter.error(sender, "Not found: " + args[0]);
        }
    }

    private void handleKbClear(ICommandSender sender) {
        int count = KnowledgeBase.size();
        KnowledgeBase.clear();
        ChatFormatter.success(sender, "Cleared " + count + " entries.");
    }

    private void handleQuery(ICommandSender sender, String[] args, boolean force) {
        if (args.length == 0) {
            ChatFormatter.error(sender, "Usage: /knows query <id> [question]");
            return;
        }

        if (!ModConfig.isConfigured()) {
            ChatFormatter.error(sender, "API not configured!");
            return;
        }

        String idOrName = args[0];
        KnowledgeEntry entry = KnowledgeBase.get(idOrName);
        if (entry == null) entry = KnowledgeBase.findByName(idOrName);

        if (entry == null) {
            ChatFormatter.error(sender, "Not found: " + idOrName);
            return;
        }

        if (!(sender instanceof EntityPlayerMP)) {
            ChatFormatter.error(sender, "Requires player.");
            return;
        }

        EntityPlayerMP player = (EntityPlayerMP) sender;
        World world = player.worldObj;

        Block currentBlock = world.getBlock(entry.getX(), entry.getY(), entry.getZ());
        GameRegistry.UniqueIdentifier uid = GameRegistry.findUniqueIdentifierFor(currentBlock);
        String currentBlockId = uid != null ? uid.toString() : currentBlock.getUnlocalizedName();

        if (!entry.getBlockId()
            .equals(currentBlockId) && !force) {
            if (ModConfig.shouldPromptOnMismatch()) {
                ChatFormatter.warning(sender, "Block mismatch: " + entry.getName());
                ChatFormatter.listItem(sender, "Was", entry.getBlockId());
                ChatFormatter.listItem(sender, "Now", currentBlockId);
                ChatFormatter.info(sender, "/knows kb remove " + entry.getId());
                ChatFormatter.info(sender, "/knows qf " + entry.getId() + " - force");
                return;
            } else {
                ChatFormatter.warning(sender, "Auto-removed: " + entry.getName());
                KnowledgeBase.remove(entry.getId());
                return;
            }
        }

        String blockData = WorldDataReader.getFullDataForAI(world, entry.getX(), entry.getY(), entry.getZ());
        String question = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length))
            : "Analyze this block and summarize its contents/status.";

        ChatFormatter.thinking(sender);

        final KnowledgeEntry fe = entry;
        OpenAIClient.chat(player.getUniqueID(), question, blockData, response -> {
            ServerScheduler.schedule(() -> {
                ChatFormatter.info(sender, "Re: " + fe.getName());
                ChatFormatter.aiResponse(sender, response);
            });
        }, error -> { ServerScheduler.schedule(() -> ChatFormatter.error(sender, error)); });
    }

    private void showCurrentConfig(ICommandSender sender) {
        ChatFormatter.header(sender, "Configuration");
        ChatFormatter.listItem(sender, "API URL", ModConfig.getApiUrl());

        String key = ModConfig.getApiKey();
        String masked = key.isEmpty() ? "(not set)"
            : key.substring(0, Math.min(8, key.length())) + "..." + key.substring(Math.max(0, key.length() - 4));
        ChatFormatter.listItem(sender, "API Key", masked);
        ChatFormatter.listItem(sender, "Model", ModConfig.getModel());
        ChatFormatter.listItem(sender, "Status", ModConfig.isConfigured() ? "Ready" : "Not configured");
    }

    private void showHelp(ICommandSender sender) {
        ChatFormatter.header(sender, "Knows It All");
        ChatFormatter.commandHelp(sender, "/knows <question>", "Ask anything (auto-context)");
        ChatFormatter.commandHelp(sender, "/knows query <id>", "Query bookmarked block");
        ChatFormatter.commandHelp(sender, "/knows kb", "Manage bookmarks");
        ChatFormatter.commandHelp(sender, "/knows clear", "Clear conversation history");
        ChatFormatter.commandHelp(sender, "/knows config", "API settings");
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, SUBCOMMANDS);
        }

        String sub = args[0].toLowerCase();
        if (args.length == 2) {
            if (sub.equals("config") || sub.equals("cfg")) {
                return getListOfStringsMatchingLastWord(args, CONFIG_OPTIONS);
            }
            if (sub.equals("kb") || sub.equals("knowledge")) {
                return getListOfStringsMatchingLastWord(args, KB_OPTIONS);
            }
            if (sub.equals("query") || sub.equals("q") || sub.equals("qf")) {
                return getKnowledgeEntryIds(args);
            }
        }

        if (args.length == 3 && (sub.equals("kb") || sub.equals("knowledge"))) {
            String action = args[1].toLowerCase();
            if (action.equals("info") || action.equals("rename") || action.equals("remove") || action.equals("rm")) {
                return getKnowledgeEntryIds(args);
            }
        }

        return new ArrayList<>();
    }

    private List<String> getKnowledgeEntryIds(String[] args) {
        List<String> ids = new ArrayList<>();
        for (KnowledgeEntry entry : KnowledgeBase.getAll()) {
            ids.add(entry.getId());
        }
        return getListOfStringsMatchingLastWord(args, ids.toArray(new String[0]));
    }
}
