package cr.chromapie.knowsitall.nei;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
        return getRecipesFor(stack, 50);
    }

    public static List<RecipeResult> getRecipesFor(ItemStack stack, int limit) {
        List<RecipeResult> results = new ArrayList<>();
        if (stack == null) return results;

        ArrayList<ICraftingHandler> handlers = GuiCraftingRecipe.getCraftingHandlers("item", stack);

        for (ICraftingHandler handler : handlers) {
            String handlerName = GuiRecipeTab.getHandlerInfo(handler).getHandlerName();
            String shortName = getShortHandlerName(handlerName);
            int numRecipes = handler.numRecipes();

            for (int i = 0; i < numRecipes && results.size() < limit; i++) {
                RecipeResult result = new RecipeResult();
                result.handlerFull = handlerName;
                result.handler = shortName;

                PositionedStack output = handler.getResultStack(i);
                if (output != null && output.item != null) {
                    result.result = output.item.getDisplayName();
                    result.resultCount = output.item.stackSize;
                } else {
                    result.result = stack.getDisplayName();
                    result.resultCount = 1;
                }

                List<PositionedStack> ingredients = handler.getIngredientStacks(i);
                for (PositionedStack ing : ingredients) {
                    if (ing != null && ing.item != null) {
                        result.ingredients.add(new Ingredient(ing.item.getDisplayName(), ing.item.stackSize));
                    }
                }

                try {
                    RecipeHandlerRef ref = RecipeHandlerRef.of(handler, i);
                    result.extra = RecipeDataExtractor.extract(ref);
                } catch (Exception ignored) {}

                if (!result.ingredients.isEmpty()) {
                    results.add(result);
                }
            }
        }

        return results;
    }

    public static String getShortHandlerName(String fullName) {
        if (fullName == null) return "Unknown";
        if (fullName.contains(".")) {
            String last = fullName.substring(fullName.lastIndexOf('.') + 1);
            return last.replace("Handler", "").replace("Recipe", "");
        }
        return fullName;
    }

    public static Map<String, List<RecipeResult>> groupByHandler(List<RecipeResult> recipes) {
        Map<String, List<RecipeResult>> map = new LinkedHashMap<>();
        for (RecipeResult r : recipes) {
            map.computeIfAbsent(r.handler, k -> new ArrayList<>()).add(r);
        }
        return map;
    }

    public static List<RecipeResult> filterByHandler(List<RecipeResult> recipes, String filter) {
        String lowerFilter = filter.toLowerCase();
        List<RecipeResult> result = new ArrayList<>();
        for (RecipeResult r : recipes) {
            if (r.handler.toLowerCase().contains(lowerFilter) ||
                r.handlerFull.toLowerCase().contains(lowerFilter)) {
                result.add(r);
            }
        }
        return result;
    }

    public static List<RecipeResult> filterByIngredient(List<RecipeResult> recipes, String filter) {
        String lowerFilter = filter.toLowerCase();
        List<RecipeResult> result = new ArrayList<>();
        for (RecipeResult r : recipes) {
            for (Ingredient ing : r.ingredients) {
                if (ing.name.toLowerCase().contains(lowerFilter)) {
                    result.add(r);
                    break;
                }
            }
        }
        return result;
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

    public static class Ingredient {
        public String name;
        public int count;

        public Ingredient(String name, int count) {
            this.name = name;
            this.count = count;
        }

        @Override
        public String toString() {
            return count + "x " + name;
        }
    }

    public static class RecipeResult {
        public String handler;
        public String handlerFull;
        public String result;
        public int resultCount;
        public List<Ingredient> ingredients = new ArrayList<>();
        public JsonObject extra;

        public String toCompactString() {
            StringBuilder sb = new StringBuilder();
            sb.append("[").append(handler).append("] ");
            for (int i = 0; i < ingredients.size(); i++) {
                if (i > 0) sb.append(" + ");
                sb.append(ingredients.get(i));
            }
            sb.append(" → ").append(resultCount).append("x ").append(result);
            if (extra != null) {
                if (extra.has("EU/t")) {
                    sb.append(" (").append(extra.get("EU/t").getAsInt()).append(" EU/t");
                    if (extra.has("duration")) {
                        sb.append(", ").append(extra.get("duration").getAsInt()).append(" ticks");
                    }
                    sb.append(")");
                }
            }
            return sb.toString();
        }

        public String toDetailedString() {
            StringBuilder sb = new StringBuilder();
            sb.append("§e").append(handler).append("§f:\n");
            sb.append("  Ingredients: ");
            for (int i = 0; i < ingredients.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(ingredients.get(i));
            }
            sb.append("\n  Result: ").append(resultCount).append("x ").append(result);
            if (extra != null && !extra.entrySet().isEmpty()) {
                sb.append("\n  Extra: ").append(extra.toString());
            }
            return sb.toString();
        }
    }
}
