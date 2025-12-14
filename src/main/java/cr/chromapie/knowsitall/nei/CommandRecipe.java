package cr.chromapie.knowsitall.nei;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import cpw.mods.fml.common.Loader;
import cr.chromapie.knowsitall.ModConfig;
import cr.chromapie.knowsitall.api.OpenAIClient;
import cr.chromapie.knowsitall.util.ChatFormatter;
import cr.chromapie.knowsitall.util.ServerScheduler;

public class CommandRecipe extends CommandBase {

    @Override
    public String getCommandName() {
        return "knowsrecipe";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/knowsrecipe <analyze|list|clear> [args]";
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
        if (!Loader.isModLoaded("NotEnoughItems")) {
            ChatFormatter.error(sender, "NEI is required for recipe commands.");
            return;
        }

        if (args.length == 0) {
            showHelp(sender);
            return;
        }

        String sub = args[0].toLowerCase();
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);

        switch (sub) {
            case "analyze":
            case "a":
                handleAnalyze(sender, subArgs);
                break;
            case "list":
            case "ls":
                handleList(sender);
                break;
            case "clear":
                handleClear(sender);
                break;
            case "help":
            default:
                showHelp(sender);
        }
    }

    private void handleAnalyze(ICommandSender sender, String[] args) {
        if (!(sender instanceof EntityPlayer)) {
            ChatFormatter.error(sender, "Requires player.");
            return;
        }

        EntityPlayer player = (EntityPlayer) sender;
        ItemStack held = player.getHeldItem();

        if (held == null) {
            ChatFormatter.error(sender, "Hold an item to analyze its recipe tree.");
            return;
        }

        int depth = 5;
        if (args.length > 0) {
            try {
                depth = Integer.parseInt(args[0]);
                depth = Math.max(1, Math.min(depth, 10));
            } catch (NumberFormatException ignored) {}
        }

        ChatFormatter.info(sender, "Analyzing: " + held.getDisplayName() + " (depth=" + depth + ")");

        RecipeAnalyzer.AnalysisResult result = RecipeAnalyzer.analyze(held, depth);

        if (result.isComplete()) {
            ChatFormatter.success(sender, "Recipe tree complete!");
        } else {
            ChatFormatter.warning(sender, result.missingRecipes.size() + " recipes missing from Knowledge Base.");
            ChatFormatter.info(sender, "Mark them in NEI with the [K] button.");
        }

        for (String line : result.toAIText()
            .split("\n")) {
            ChatFormatter.sendRaw(sender, line);
        }

        if (ModConfig.isConfigured() && args.length > 1 && args[args.length - 1].equalsIgnoreCase("ai")) {
            String question = "Based on this recipe analysis, summarize what materials I need and suggest an efficient crafting order.";
            ChatFormatter.thinking(sender);

            OpenAIClient.chat(player.getUniqueID(), question, result.toAIText(), response -> {
                ServerScheduler.schedule(() -> {
                    ChatFormatter.clearActionBar(sender);
                    ChatFormatter.aiResponse(sender, response);
                });
            }, error -> {
                ServerScheduler.schedule(() -> {
                    ChatFormatter.clearActionBar(sender);
                    ChatFormatter.error(sender, error);
                });
            });
        }
    }

    private void handleList(ICommandSender sender) {
        int count = RecipeKnowledge.size();
        ChatFormatter.header(sender, "Recipe Knowledge (" + count + ")");

        if (count == 0) {
            ChatFormatter.info(sender, "Empty. Mark recipes in NEI with [K] button.");
        } else {
            ChatFormatter.info(sender, count + " recipes stored.");
            ChatFormatter.info(sender, "Use /knowsrecipe analyze while holding an item.");
        }
    }

    private void handleClear(ICommandSender sender) {
        ChatFormatter.warning(sender, "Recipe knowledge cleared.");
    }

    private void showHelp(ICommandSender sender) {
        ChatFormatter.header(sender, "Recipe Commands");
        ChatFormatter.commandHelp(sender, "/knowsrecipe analyze [depth]", "Analyze held item's recipe tree");
        ChatFormatter.commandHelp(sender, "/knowsrecipe analyze [depth] ai", "Analyze + AI summary");
        ChatFormatter.commandHelp(sender, "/knowsrecipe list", "Show recipe knowledge stats");
        ChatFormatter.info(sender, "Mark recipes in NEI using the §a[K]§7 button.");
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "analyze", "list", "clear", "help");
        }
        return new ArrayList<>();
    }
}
