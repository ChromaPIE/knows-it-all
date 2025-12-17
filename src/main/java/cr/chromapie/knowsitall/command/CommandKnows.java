package cr.chromapie.knowsitall.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;

import cr.chromapie.knowsitall.api.ConversationManager;
import cr.chromapie.knowsitall.knowledge.KnowledgeBase;
import cr.chromapie.knowsitall.knowledge.KnowledgeEntry;
import cr.chromapie.knowsitall.ui.ChatScreen;
import cr.chromapie.knowsitall.util.ChatFormatter;
import cr.chromapie.knowsitall.util.ServerScheduler;

public class CommandKnows extends CommandBase {

    private static final String[] SUBCOMMANDS = { "kb", "clear", "help" };
    private static final String[] KB_OPTIONS = { "list", "rename", "remove", "clear", "info" };

    @Override
    public String getCommandName() {
        return "knows";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/knows <kb|clear|help>";
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
            case "kb":
            case "knowledge":
                handleKnowledgeBase(sender, subArgs);
                break;
            case "clear":
                handleClear(sender);
                break;
            case "help":
            case "?":
                showHelp(sender);
                break;
            default:
                ChatFormatter.error(sender, "Unknown command: " + subCommand);
                showHelp(sender);
        }
    }

    private void handleClear(ICommandSender sender) {
        if (!(sender instanceof EntityPlayerMP player)) {
            ChatFormatter.error(sender, "Requires player.");
            return;
        }
        int count = ConversationManager.getMessageCount(player.getUniqueID());
        ConversationManager.clear(player.getUniqueID());
        ServerScheduler.scheduleClient(ChatScreen::clearMessages);
        ChatFormatter.success(sender, "Cleared " + count + " messages from history.");
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

    private void showHelp(ICommandSender sender) {
        ChatFormatter.header(sender, "Knows It All");
        ChatFormatter.commandHelp(sender, "[K]", "Open chat interface (configurable)");
        ChatFormatter.commandHelp(sender, "/knows kb", "Manage bookmarks");
        ChatFormatter.commandHelp(sender, "/knows clear", "Clear conversation history");
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, SUBCOMMANDS);
        }

        String sub = args[0].toLowerCase();
        if (args.length == 2 && (sub.equals("kb") || sub.equals("knowledge"))) {
            return getListOfStringsMatchingLastWord(args, KB_OPTIONS);
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
