package cr.chromapie.knowsitall.ui;

import java.util.ArrayList;
import java.util.List;

import com.cleanroommc.modularui.widgets.textfield.TextFieldHandler;
import com.cleanroommc.modularui.widgets.textfield.TextFieldRenderer;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class PasswordFieldWidget extends TextFieldWidget {

    public PasswordFieldWidget() {
        super();
        this.renderer = new MaskedRenderer(this.handler);
        setTooltipOverride(true);
    }

    private static class MaskedRenderer extends TextFieldRenderer {

        public MaskedRenderer(TextFieldHandler handler) {
            super(handler);
        }

        @Override
        public void draw(List<String> lines) {
            List<String> masked = new ArrayList<>(lines.size());
            for (String line : lines) {
                masked.add(maskString(line));
            }
            super.draw(masked);
        }

        private String maskString(String text) {
            if (text == null || text.isEmpty()) {
                return text;
            }
            StringBuilder sb = new StringBuilder(text.length());
            for (int i = 0; i < text.length(); i++) {
                sb.append('*');
            }
            return sb.toString();
        }
    }
}
