package cr.chromapie.knowsitall.nei;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.item.ItemStack;

import codechicken.nei.recipe.Recipe;
import codechicken.nei.recipe.Recipe.RecipeId;
import codechicken.nei.recipe.Recipe.RecipeIngredient;
import codechicken.nei.recipe.StackInfo;

public class RecipeAnalyzer {

    public static class AnalysisResult {

        public final ItemStack target;
        public final List<RecipeNode> recipeTree;
        public final List<ItemStack> missingRecipes;
        public final Map<String, Integer> totalMaterials;

        public AnalysisResult(ItemStack target, List<RecipeNode> tree, List<ItemStack> missing) {
            this.target = target;
            this.recipeTree = tree;
            this.missingRecipes = missing;
            this.totalMaterials = calculateTotals(tree, missing);
        }

        private Map<String, Integer> calculateTotals(List<RecipeNode> tree, List<ItemStack> missing) {
            Map<String, Integer> totals = new HashMap<>();

            for (ItemStack stack : missing) {
                String key = getItemKey(stack);
                totals.merge(key, stack.stackSize, Integer::sum);
            }

            return totals;
        }

        public String toAIText() {
            StringBuilder sb = new StringBuilder();
            sb.append("=== Recipe Analysis for: ")
                .append(target.getDisplayName())
                .append(" ===\n\n");

            if (!recipeTree.isEmpty()) {
                sb.append("Recipe Chain:\n");
                for (RecipeNode node : recipeTree) {
                    sb.append(node.toText(0));
                }
            }

            if (!missingRecipes.isEmpty()) {
                sb.append("\n§cMissing Recipes (not in Knowledge Base):§r\n");
                for (ItemStack stack : missingRecipes) {
                    sb.append("  - ")
                        .append(stack.getDisplayName());
                    if (stack.stackSize > 1) {
                        sb.append(" x")
                            .append(stack.stackSize);
                    }
                    sb.append("\n");
                }
            }

            if (!totalMaterials.isEmpty()) {
                sb.append("\n§eBase Materials Needed:§r\n");
                for (Map.Entry<String, Integer> entry : totalMaterials.entrySet()) {
                    sb.append("  - ")
                        .append(entry.getKey());
                    if (entry.getValue() > 1) {
                        sb.append(" x")
                            .append(entry.getValue());
                    }
                    sb.append("\n");
                }
            }

            return sb.toString();
        }

        public boolean isComplete() {
            return missingRecipes.isEmpty();
        }
    }

    public static class RecipeNode {

        public final ItemStack result;
        public final String handlerName;
        public final List<ItemStack> ingredients;
        public final List<RecipeNode> children;

        public RecipeNode(ItemStack result, String handlerName, List<ItemStack> ingredients) {
            this.result = result;
            this.handlerName = handlerName;
            this.ingredients = ingredients;
            this.children = new ArrayList<>();
        }

        public String toText(int depth) {
            StringBuilder sb = new StringBuilder();
            StringBuilder indentBuilder = new StringBuilder();
            for (int i = 0; i < depth; i++) indentBuilder.append("  ");
            String indent = indentBuilder.toString();

            sb.append(indent)
                .append("§b")
                .append(result.getDisplayName())
                .append("§r");
            if (result.stackSize > 1) {
                sb.append(" x")
                    .append(result.stackSize);
            }
            sb.append(" [")
                .append(simplifyHandlerName(handlerName))
                .append("]\n");

            for (ItemStack ing : ingredients) {
                sb.append(indent)
                    .append("  <- §7")
                    .append(ing.getDisplayName());
                if (ing.stackSize > 1) {
                    sb.append(" x")
                        .append(ing.stackSize);
                }
                sb.append("§r\n");
            }

            for (RecipeNode child : children) {
                sb.append(child.toText(depth + 1));
            }

            return sb.toString();
        }

        private String simplifyHandlerName(String name) {
            if (name == null) return "Unknown";
            int lastDot = name.lastIndexOf('.');
            return lastDot >= 0 ? name.substring(lastDot + 1) : name;
        }
    }

    public static AnalysisResult analyze(ItemStack target, int maxDepth) {
        List<RecipeNode> tree = new ArrayList<>();
        List<ItemStack> missing = new ArrayList<>();
        Set<String> visited = new HashSet<>();

        analyzeRecursive(target, tree, missing, visited, maxDepth);

        return new AnalysisResult(target, tree, missing);
    }

    private static void analyzeRecursive(ItemStack stack, List<RecipeNode> tree, List<ItemStack> missing,
        Set<String> visited, int depth) {

        if (depth < 0) return;

        String key = getItemKey(stack);
        if (visited.contains(key)) return;
        visited.add(key);

        RecipeId recipeId = RecipeKnowledge.get(stack);
        if (recipeId == null) {
            missing.add(stack.copy());
            return;
        }

        Recipe recipe = Recipe.of(recipeId);
        if (recipe == null) {
            missing.add(stack.copy());
            return;
        }

        List<ItemStack> ingredients = new ArrayList<>();
        for (RecipeIngredient ing : recipe.getIngredients()) {
            ingredients.add(
                ing.getItemStack()
                    .copy());
        }

        RecipeNode node = new RecipeNode(stack.copy(), recipe.getHandlerName(), ingredients);
        tree.add(node);

        for (ItemStack ing : ingredients) {
            analyzeRecursive(ing, node.children, missing, visited, depth - 1);
        }
    }

    private static String getItemKey(ItemStack stack) {
        return StackInfo.getItemStackGUID(stack);
    }
}
