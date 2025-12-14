package cr.chromapie.knowsitall.nei.adapter;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import codechicken.nei.recipe.IRecipeHandler;
import codechicken.nei.recipe.RecipeHandlerRef;
import cpw.mods.fml.common.Loader;

public class RecipeDataExtractor {

    public static JsonObject extract(RecipeHandlerRef handlerRef) {
        JsonObject extra = new JsonObject();

        IRecipeHandler handler = handlerRef.handler;
        String handlerClass = handler.getClass()
            .getName();

        if (Loader.isModLoaded("gregtech") && handlerClass.contains("gregtech")) {
            try {
                extractGT(handler, handlerRef.recipeIndex, extra);
            } catch (Exception ignored) {}
        }

        return extra.entrySet()
            .isEmpty() ? null : extra;
    }

    private static void extractGT(IRecipeHandler handler, int recipeIndex, JsonObject extra) throws Exception {
        if (handler instanceof gregtech.nei.GTNEIDefaultHandler gtHandler) {
            var cachedRecipes = gtHandler.arecipes;
            if (recipeIndex < cachedRecipes.size()) {
                var cached = cachedRecipes.get(recipeIndex);
                if (cached instanceof gregtech.nei.GTNEIDefaultHandler.CachedDefaultRecipe gtCached) {
                    gregtech.api.util.GTRecipe recipe = gtCached.mRecipe;

                    extra.addProperty("EU/t", recipe.mEUt);
                    extra.addProperty("duration", recipe.mDuration);
                    extra.addProperty("totalEU", (long) recipe.mEUt * recipe.mDuration);

                    if (recipe.mFluidInputs != null && recipe.mFluidInputs.length > 0) {
                        JsonArray fluids = new JsonArray();
                        for (var fluid : recipe.mFluidInputs) {
                            if (fluid != null) {
                                JsonObject f = new JsonObject();
                                f.addProperty("name", fluid.getLocalizedName());
                                f.addProperty("amount", fluid.amount);
                                fluids.add(f);
                            }
                        }
                        if (fluids.size() > 0) extra.add("fluidInputs", fluids);
                    }

                    if (recipe.mFluidOutputs != null && recipe.mFluidOutputs.length > 0) {
                        JsonArray fluids = new JsonArray();
                        for (var fluid : recipe.mFluidOutputs) {
                            if (fluid != null) {
                                JsonObject f = new JsonObject();
                                f.addProperty("name", fluid.getLocalizedName());
                                f.addProperty("amount", fluid.amount);
                                fluids.add(f);
                            }
                        }
                        if (fluids.size() > 0) extra.add("fluidOutputs", fluids);
                    }
                }
            }
        }
    }
}
