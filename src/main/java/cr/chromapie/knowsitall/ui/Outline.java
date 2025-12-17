package cr.chromapie.knowsitall.ui;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.screen.viewport.GuiContext;
import com.cleanroommc.modularui.theme.WidgetTheme;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class Outline implements IDrawable {

    private final int fillColor;
    private final int borderColor;
    private final int borderWidth;

    public Outline(int fillColor, int borderColor, int borderWidth) {
        this.fillColor = fillColor;
        this.borderColor = borderColor;
        this.borderWidth = borderWidth;
    }

    public Outline(int fillColor, int borderColor) {
        this(fillColor, borderColor, 1);
    }

    @Override
    public void draw(GuiContext context, int x, int y, int width, int height, WidgetTheme widgetTheme) {
        GuiDraw.drawRect(x, y, width, borderWidth, borderColor);
        GuiDraw.drawRect(x, y + height - borderWidth, width, borderWidth, borderColor);
        GuiDraw.drawRect(x, y, borderWidth, height, borderColor);
        GuiDraw.drawRect(x + width - borderWidth, y, borderWidth, height, borderColor);

        int innerX = x + borderWidth;
        int innerY = y + borderWidth;
        int innerW = width - borderWidth * 2;
        int innerH = height - borderWidth * 2;
        if (innerW > 0 && innerH > 0) {
            GuiDraw.drawRect(innerX, innerY, innerW, innerH, fillColor);
        }
    }
}
