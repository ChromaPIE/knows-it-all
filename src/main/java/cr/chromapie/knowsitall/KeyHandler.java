package cr.chromapie.knowsitall;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;

import org.lwjgl.input.Keyboard;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import cr.chromapie.knowsitall.ui.ChatScreen;

@SideOnly(Side.CLIENT)
public class KeyHandler {

    public static final KeyBinding OPEN_CHAT = new KeyBinding(
        "key.knowsitall.open_chat",
        Keyboard.KEY_K,
        "key.categories.knowsitall");

    public static void register() {
        ClientRegistry.registerKeyBinding(OPEN_CHAT);
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen != null) {
            return;
        }

        if (OPEN_CHAT.isPressed()) {
            ChatScreen.open();
        }
    }
}
