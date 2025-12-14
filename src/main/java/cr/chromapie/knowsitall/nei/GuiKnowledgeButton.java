package cr.chromapie.knowsitall.nei;

import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.item.ItemStack;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import codechicken.nei.recipe.GuiRecipeButton;
import codechicken.nei.recipe.Recipe;
import codechicken.nei.recipe.Recipe.RecipeIngredient;
import codechicken.nei.recipe.RecipeHandlerRef;
import cr.chromapie.knowsitall.nei.adapter.RecipeDataExtractor;

public class GuiKnowledgeButton extends GuiRecipeButton {

    private static final int BUTTON_ID_START = 100;
    private static final int TEXT_ON = 0xFFFFFF55;
    private static final int TEXT_OFF = 0xFFFFFFFF;
    private static final int BG_ON = 0xFFFF9900;

    private final Recipe recipe;
    private final RecipeHandlerRef handlerRef;
    private boolean inKnowledge;

    public GuiKnowledgeButton(RecipeHandlerRef handlerRef, int x, int y) {
        super(handlerRef, x, y, BUTTON_ID_START + handlerRef.recipeIndex, "K");
        this.handlerRef = handlerRef;
        this.recipe = Recipe.of(handlerRef);

        ItemStack result = this.recipe.getResult();
        this.inKnowledge = result != null && RecipeKnowledge.contains(result);
        this.visible = result != null;
    }

    @Override
    public void update() {
        ItemStack result = this.recipe.getResult();
        this.inKnowledge = result != null && RecipeKnowledge.contains(result)
            && this.recipe.getRecipeId()
                .equals(RecipeKnowledge.get(result));
    }

    @Override
    protected void drawContent(Minecraft minecraft, int y, int x, boolean mouseOver) {
        if (this.inKnowledge) {
            drawRect(this.xPosition, this.yPosition, this.xPosition + this.width, this.yPosition + this.height, BG_ON);
        }

        FontRenderer fr = minecraft.fontRenderer;
        int color = this.inKnowledge ? TEXT_ON : TEXT_OFF;
        if (!this.enabled) {
            color = 0xFF666666;
        }

        String text = "K";
        int textX = this.xPosition + (this.width - fr.getStringWidth(text)) / 2;
        int textY = this.yPosition + (this.height - 8) / 2;
        fr.drawStringWithShadow(text, textX, textY, color);
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY) {
        ItemStack result = this.recipe.getResult();
        if (result == null) return;

        this.inKnowledge = !this.inKnowledge;
        if (this.inKnowledge) {
            JsonObject recipeData = serializeRecipe();
            RecipeKnowledge.setWithData(result, this.recipe.getRecipeId(), recipeData);
        } else {
            RecipeKnowledge.set(result, null);
        }
    }

    private JsonObject serializeRecipe() {
        JsonObject obj = new JsonObject();
        obj.addProperty("handler", this.recipe.getHandlerName());

        JsonArray ingredients = new JsonArray();
        for (RecipeIngredient ing : this.recipe.getIngredients()) {
            ItemStack stack = ing.getItemStack();
            if (stack == null) continue;
            JsonObject ingObj = new JsonObject();
            ingObj.addProperty("name", stack.getDisplayName());
            ingObj.addProperty("count", stack.stackSize);
            ingredients.add(ingObj);
        }
        obj.add("ingredients", ingredients);

        ItemStack result = this.recipe.getResult();
        if (result != null) {
            obj.addProperty("result", result.getDisplayName());
            obj.addProperty("resultCount", result.stackSize);
        }

        try {
            JsonObject extra = RecipeDataExtractor.extract(this.handlerRef);
            if (extra != null) {
                obj.add("extra", extra);
            }
        } catch (Exception ignored) {}

        return obj;
    }

    @Override
    public List<String> handleTooltip(List<String> currenttip) {
        currenttip.add(this.inKnowledge ? "§aIn Knowledge Base" : "§7Add to Knowledge Base");
        return currenttip;
    }

    @Override
    public Map<String, String> handleHotkeys(int mousex, int mousey, Map<String, String> hotkeys) {
        return hotkeys;
    }

    @Override
    public void lastKeyTyped(char keyChar, int keyID) {}

    @Override
    public void drawItemOverlay() {}
}
