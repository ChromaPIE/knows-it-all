package cr.chromapie.knowsitall.nei;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;

import com.google.gson.JsonObject;

import codechicken.nei.ItemList;
import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.GuiCraftingRecipe;
import codechicken.nei.recipe.GuiRecipeTab;
import codechicken.nei.recipe.ICraftingHandler;
import codechicken.nei.recipe.RecipeHandlerRef;
import cr.chromapie.knowsitall.nei.adapter.RecipeDataExtractor;

public class NEIRecipeQuery {

    public static List<RecipeResult> getRecipesFor(ItemStack stack) {
        List<RecipeResult> results = new ArrayList<>();
        if (stack == null) return results;

        ArrayList<ICraftingHandler> handlers = GuiCraftingRecipe.getCraftingHandlers("item", stack);

        for (ICraftingHandler handler : handlers) {
            String handlerName = GuiRecipeTab.getHandlerInfo(handler)
                .getHandlerName();
            int numRecipes = handler.numRecipes();

            for (int i = 0; i < numRecipes && results.size() < 10; i++) {
                RecipeResult result = new RecipeResult();
                result.handler = handlerName;

                PositionedStack output = handler.getResultStack(i);
                if (output != null && output.item != null) {
                    result.result = output.item.getDisplayName();
                    result.resultCount = output.item.stackSize;
                }

                List<PositionedStack> ingredients = handler.getIngredientStacks(i);
                for (PositionedStack ing : ingredients) {
                    if (ing != null && ing.item != null) {
                        result.ingredients.add(ing.item.stackSize + "x " + ing.item.getDisplayName());
                    }
                }

                try {
                    RecipeHandlerRef ref = RecipeHandlerRef.of(handler, i);
                    result.extra = RecipeDataExtractor.extract(ref);
                } catch (Exception ignored) {}

                results.add(result);
            }
        }

        return results;
    }

    public static ItemStack findItemByName(String query) {
        String lowerQuery = query.toLowerCase();
        String[] words = lowerQuery.split("\\s+");
        StringBuilder patternBuilder = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            if (i > 0) patternBuilder.append(".*");
            patternBuilder.append(java.util.regex.Pattern.quote(words[i]));
        }
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(patternBuilder.toString());

        try {
            for (ItemStack stack : ItemList.items) {
                if (stack == null) continue;
                String name = stack.getDisplayName();
                if (name != null && pattern.matcher(name.toLowerCase()).find()) {
                    return stack.copy();
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    public static String queryRecipeByName(String query) {
        ItemStack stack = findItemByName(query);
        if (stack == null) return null;

        StringBuilder sb = new StringBuilder();
        sb.append("[NEI] Recipes for ")
            .append(stack.getDisplayName())
            .append(":\n");

        List<RecipeResult> recipes = getRecipesFor(stack);
        if (recipes.isEmpty()) return null;

        int count = 0;
        for (RecipeResult r : recipes) {
            sb.append("---\n")
                .append(r.toString())
                .append("\n");
            count++;
            if (count >= 3) break;
        }
        return sb.toString();
    }

    public static class RecipeResult {

        public String handler;
        public String result;
        public int resultCount;
        public List<String> ingredients = new ArrayList<>();
        public JsonObject extra;

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Handler: ")
                .append(handler)
                .append("\n");
            sb.append("Result: ")
                .append(resultCount)
                .append("x ")
                .append(result)
                .append("\n");
            sb.append("Ingredients: ")
                .append(String.join(", ", ingredients));
            if (extra != null) {
                sb.append("\nExtra: ")
                    .append(extra.toString());
            }
            return sb.toString();
        }
    }
}
