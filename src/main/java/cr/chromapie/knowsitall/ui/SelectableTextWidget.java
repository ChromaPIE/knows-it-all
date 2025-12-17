package cr.chromapie.knowsitall.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.IntSupplier;

import net.minecraft.client.gui.GuiScreen;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IFocusedWidget;
import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.drawable.text.TextRenderer;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetTheme;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.utils.Color;
import com.cleanroommc.modularui.utils.GlStateManager;
import com.cleanroommc.modularui.utils.Platform;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widget.sizer.Box;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class SelectableTextWidget extends Widget<SelectableTextWidget> implements IFocusedWidget, Interactable {

    private final IKey key;
    private Alignment alignment = Alignment.TopLeft;
    private IntSupplier textColor = null;
    private float scale = 1f;
    private int maxWidth = -1;
    private int markedColor = 0xFF2F72A8;
    private int siblingWidth = 0;

    private List<String> lines = new ArrayList<>();
    private boolean selectAll = false;

    public SelectableTextWidget(IKey key) {
        this.key = key;
    }

    public SelectableTextWidget(String text) {
        this(IKey.str(text));
    }

    @Override
    public void onInit() {
        super.onInit();
        updateLines();
    }

    private void updateLines() {
        String text = this.key.getFormatted();
        this.lines = new ArrayList<>(Arrays.asList(text.split("\n", -1)));
        if (this.lines.isEmpty()) {
            this.lines.add("");
        }
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
        updateLines();

        TextRenderer renderer = TextRenderer.SHARED;
        WidgetTheme theme = getActiveWidgetTheme(widgetTheme, isHovering());
        int color = this.textColor != null ? this.textColor.getAsInt() : theme.getTextColor();

        Box padding = getArea().getPadding();
        float maxW = getMaxWidthForRender();

        renderer.setColor(color);
        renderer.setAlignment(this.alignment, maxW, getArea().paddedHeight());
        renderer.setPos(padding.getLeft(), padding.getTop());
        renderer.setScale(this.scale);
        renderer.setSimulate(false);

        if (selectAll && isFocused()) {
            drawFullSelection(renderer, padding);
        }

        renderer.draw(this.lines);
    }

    private float getMaxWidthForRender() {
        if (this.maxWidth > 0) {
            return Math.max(this.maxWidth, 5);
        }
        float w = getArea().paddedWidth();
        if (w > 0) {
            return w;
        }
        return getEffectiveWidth();
    }

    private float getEffectiveWidth() {
        Box padding = getArea().getPadding();
        int baseWidth = 300;
        return Math.max(50, baseWidth - this.siblingWidth - padding.horizontal());
    }

    private void drawFullSelection(TextRenderer renderer, Box padding) {
        renderer.setSimulate(true);
        renderer.draw(this.lines);
        renderer.setSimulate(false);

        float fontHeight = 9 * this.scale;
        float startY = padding.getTop();

        for (int lineIdx = 0; lineIdx < this.lines.size(); lineIdx++) {
            String line = this.lines.get(lineIdx);
            float lineY = startY + lineIdx * fontHeight;

            String visibleLine = getVisibleText(line);
            float lineWidth = TextRenderer.getFontRenderer()
                .getStringWidth(visibleLine) * this.scale;

            if (lineWidth > 0) {
                drawSelectionRect(padding.getLeft(), lineY - 1, padding.getLeft() + lineWidth, lineY + fontHeight - 1);
            }
        }
    }

    private String getVisibleText(String text) {
        StringBuilder sb = new StringBuilder();
        boolean skipNext = false;
        for (char c : text.toCharArray()) {
            if (skipNext) {
                skipNext = false;
                continue;
            }
            if (c == '§') {
                skipNext = true;
                continue;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private void drawSelectionRect(float x0, float y0, float x1, float y1) {
        float red = Color.getRedF(this.markedColor);
        float green = Color.getGreenF(this.markedColor);
        float blue = Color.getBlueF(this.markedColor);
        float alpha = Color.getAlphaF(this.markedColor);
        if (alpha == 0) alpha = 1f;

        Platform.setupDrawColor();
        GlStateManager.color(red, green, blue, alpha);
        Platform.startDrawing(Platform.DrawMode.QUADS, Platform.VertexFormat.POS, bufferBuilder -> {
            bufferBuilder.pos(x0, y1, 0.0D)
                .endVertex();
            bufferBuilder.pos(x1, y1, 0.0D)
                .endVertex();
            bufferBuilder.pos(x1, y0, 0.0D)
                .endVertex();
            bufferBuilder.pos(x0, y0, 0.0D)
                .endVertex();
        });
        GlStateManager.color(1, 1, 1, 1);
    }

    @Override
    public int getDefaultHeight() {
        updateLines();
        Box padding = getArea().getPadding();
        float fontHeight = 9 * this.scale;

        float availableWidth = getEffectiveWidth();
        TextRenderer renderer = TextRenderer.SHARED;
        int wrapWidth = Math.max(10, (int) (availableWidth / this.scale));
        int totalVisualLines = 0;

        for (String line : this.lines) {
            if (line.isEmpty()) {
                totalVisualLines++;
                continue;
            }
            List<String> wrappedLines = TextRenderer.getFontRenderer()
                .listFormattedStringToWidth(line, wrapWidth);
            totalVisualLines += Math.max(1, wrappedLines.size());
        }

        return (int) Math.ceil(totalVisualLines * fontHeight + padding.vertical());
    }

    @Override
    public int getDefaultWidth() {
        updateLines();
        Box padding = getArea().getPadding();
        float maxW = 0;
        for (String line : this.lines) {
            float w = TextRenderer.getFontRenderer()
                .getStringWidth(line) * this.scale;
            if (w > maxW) maxW = w;
        }
        return (int) Math.ceil(maxW + padding.horizontal());
    }

    @Override
    public Result onMousePressed(int mouseButton) {
        if (mouseButton == 0) {
            getContext().focus(this);
            return Result.SUCCESS;
        }
        return Result.IGNORE;
    }

    @Override
    public Result onKeyPressed(char character, int keyCode) {
        if (!isFocused()) {
            return Result.IGNORE;
        }

        if (Interactable.isKeyComboCtrlC(keyCode)) {
            if (selectAll) {
                GuiScreen.setClipboardString(getAllText());
            }
            return Result.SUCCESS;
        }

        if (Interactable.isKeyComboCtrlA(keyCode)) {
            selectAll = true;
            return Result.SUCCESS;
        }

        return Result.IGNORE;
    }

    private String getAllText() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < this.lines.size(); i++) {
            if (i > 0) sb.append("\n");
            sb.append(this.lines.get(i));
        }
        return sb.toString();
    }

    @Override
    public boolean isFocused() {
        return getContext().isFocused(this);
    }

    @Override
    public void onFocus(ModularGuiContext context) {}

    @Override
    public void onRemoveFocus(ModularGuiContext context) {
        selectAll = false;
    }

    @Override
    public boolean canHoverThrough() {
        return true;
    }

    public SelectableTextWidget alignment(Alignment alignment) {
        this.alignment = alignment;
        return this;
    }

    public SelectableTextWidget textColor(int color) {
        return textColor(() -> color);
    }

    public SelectableTextWidget textColor(IntSupplier color) {
        this.textColor = color;
        return this;
    }

    public SelectableTextWidget scale(float scale) {
        this.scale = scale;
        return this;
    }

    public SelectableTextWidget maxWidth(int maxWidth) {
        this.maxWidth = maxWidth;
        return this;
    }

    public SelectableTextWidget markedColor(int color) {
        this.markedColor = color;
        return this;
    }

    public SelectableTextWidget siblingWidth(int width) {
        this.siblingWidth = width;
        return this;
    }
}
