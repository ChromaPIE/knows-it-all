package cr.chromapie.knowsitall.nei;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.google.gson.JsonObject;

import codechicken.nei.recipe.Recipe.RecipeId;
import codechicken.nei.recipe.StackInfo;
import codechicken.nei.util.NBTJson;
import cr.chromapie.knowsitall.knowledge.KnowledgeBase;
import cr.chromapie.knowsitall.knowledge.KnowledgeEntry;

public class RecipeKnowledge {

    public static void set(ItemStack stack, RecipeId recipeId) {
        setWithData(stack, recipeId, null);
    }

    public static void setWithData(ItemStack stack, RecipeId recipeId, JsonObject recipeContent) {
        String itemKey = getKey(stack);
        KnowledgeEntry existing = KnowledgeBase.findRecipeByItemKey(itemKey);

        if (recipeId == null) {
            if (existing != null) {
                KnowledgeBase.remove(existing.getId());
            }
            return;
        }

        if (existing != null) {
            KnowledgeBase.remove(existing.getId());
        }

        String name = stack.getDisplayName() + " recipe";
        JsonObject data = new JsonObject();
        data.addProperty("itemKey", itemKey);
        data.add("recipeId", recipeId.toJsonObject());
        if (recipeContent != null) {
            data.add("recipe", recipeContent);
        }
        KnowledgeEntry entry = new KnowledgeEntry(KnowledgeBase.generateId(), "recipe", name, data);
        KnowledgeBase.add(entry);
    }

    public static RecipeId get(ItemStack stack) {
        KnowledgeEntry entry = KnowledgeBase.findRecipeByItemKey(getKey(stack));
        if (entry == null) return null;

        JsonObject data = entry.getData();
        if (data.has("recipeId")) {
            return RecipeId.of(data.getAsJsonObject("recipeId"));
        }
        return null;
    }

    public static boolean contains(ItemStack stack) {
        return KnowledgeBase.findRecipeByItemKey(getKey(stack)) != null;
    }

    public static void remove(ItemStack stack) {
        KnowledgeEntry entry = KnowledgeBase.findRecipeByItemKey(getKey(stack));
        if (entry != null) {
            KnowledgeBase.remove(entry.getId());
        }
    }

    public static int size() {
        return KnowledgeBase.sizeByType("recipe");
    }

    private static String getKey(ItemStack stack) {
        NBTTagCompound nbt = StackInfo.itemStackToNBT(stack, false);
        return NBTJson.toJson(NBTJson.toJsonObject(nbt));
    }
}
