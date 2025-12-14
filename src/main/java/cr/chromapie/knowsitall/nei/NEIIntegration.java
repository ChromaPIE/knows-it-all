package cr.chromapie.knowsitall.nei;

import codechicken.nei.recipe.GuiRecipeButton;
import codechicken.nei.recipe.GuiRecipeButton.UpdateRecipeButtonsEvent;
import codechicken.nei.recipe.RecipeHandlerRef;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class NEIIntegration {

    @SubscribeEvent
    public void onUpdateRecipeButtons(UpdateRecipeButtonsEvent.Post event) {
        if (event.buttonList.isEmpty()) return;

        GuiRecipeButton lastButton = event.buttonList.get(event.buttonList.size() - 1);
        RecipeHandlerRef handlerRef = lastButton.handlerRef;

        int x = lastButton.xPosition;
        int y = lastButton.yPosition - GuiRecipeButton.BUTTON_HEIGHT - 1;

        event.buttonList.add(new GuiKnowledgeButton(handlerRef, x, y));
    }
}
