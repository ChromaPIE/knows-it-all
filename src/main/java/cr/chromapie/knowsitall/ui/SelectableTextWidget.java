package cr.chromapie.knowsitall.ui;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.IntSupplier;

import net.minecraft.client.gui.GuiScreen;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IFocusedWidget;
import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.api.widget.Interactable.Result;
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
    private final Point selStart = new Point();
    private final Point selEnd = new Point();
    private boolean hasSelection = false;
    private boolean isDragging = false;

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

        if (hasSelection && isFocused()) {
            drawSelection(renderer, padding);
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

    private void drawSelection(TextRenderer renderer, Box padding) {
        Point start = getSelectionStart();
        Point end = getSelectionEnd();

        if (start.equals(end)) return;

        renderer.setSimulate(true);
        renderer.draw(this.lines);
        renderer.setSimulate(false);

        float fontHeight = 9 * this.scale;
        float startY = padding.getTop();

        for (int lineIdx = start.y; lineIdx <= end.y && lineIdx < this.lines.size(); lineIdx++) {
            String line = this.lines.get(lineIdx);
            float lineY = startY + lineIdx * fontHeight;

            int charStart = (lineIdx == start.y) ? start.x : 0;
            int charEnd = (lineIdx == end.y) ? end.x : line.length();

            charStart = Math.min(charStart, line.length());
            charEnd = Math.min(charEnd, line.length());

            if (charStart >= charEnd && lineIdx == start.y && lineIdx == end.y) continue;

            String beforeSel = getVisibleText(line.substring(0, charStart));
            String selected = getVisibleText(line.substring(0, charEnd));

            float x0 = padding.getLeft() + renderer.getFontRenderer()
                .getStringWidth(beforeSel) * this.scale;
            float x1 = padding.getLeft() + renderer.getFontRenderer()
                .getStringWidth(selected) * this.scale;

            drawSelectionRect(x0, lineY - 1, x1, lineY + fontHeight - 1);
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
            if (c == '\u00A7') {
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
            List<String> wrappedLines = renderer.getFontRenderer()
                .listFormattedStringToWidth(line, wrapWidth);
            totalVisualLines += Math.max(1, wrappedLines.size());
        }

        return (int) Math.ceil(totalVisualLines * fontHeight + padding.vertical());
    }

    @Override
    public int getDefaultWidth() {
        updateLines();
        Box padding = getArea().getPadding();
        TextRenderer renderer = TextRenderer.SHARED;
        float maxW = 0;
        for (String line : this.lines) {
            float w = renderer.getFontRenderer()
                .getStringWidth(line) * this.scale;
            if (w > maxW) maxW = w;
        }
        return (int) Math.ceil(maxW + padding.horizontal());
    }

    private Point getCursorPosFromMouse(int mouseX, int mouseY) {
        Box padding = getArea().getPadding();
        TextRenderer renderer = TextRenderer.SHARED;

        float fontHeight = 9 * this.scale;
        int relY = mouseY - padding.getTop();
        int lineIdx = (int) (relY / fontHeight);
        lineIdx = Math.max(0, Math.min(lineIdx, this.lines.size() - 1));

        String line = this.lines.get(lineIdx);
        int relX = mouseX - padding.getLeft();

        int charIdx = 0;
        float currentX = 0;
        String visibleLine = "";
        boolean skipNext = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (skipNext) {
                skipNext = false;
                continue;
            }
            if (c == '\u00A7') {
                skipNext = true;
                continue;
            }
            visibleLine += c;
            float newX = renderer.getFontRenderer()
                .getStringWidth(visibleLine) * this.scale;
            if (newX > relX) {
                if (relX - currentX < newX - relX) {
                    break;
                }
                charIdx++;
                break;
            }
            currentX = newX;
            charIdx++;
        }

        int actualCharIdx = 0;
        int visibleCount = 0;
        skipNext = false;
        for (int i = 0; i < line.length() && visibleCount < charIdx; i++) {
            char c = line.charAt(i);
            if (skipNext) {
                skipNext = false;
                actualCharIdx = i + 1;
                continue;
            }
            if (c == '\u00A7') {
                skipNext = true;
                actualCharIdx = i;
                continue;
            }
            visibleCount++;
            actualCharIdx = i + 1;
        }

        return new Point(Math.min(actualCharIdx, line.length()), lineIdx);
    }

    @Override
    public Result onMousePressed(int mouseButton) {
        if (!isHovering()) {
            return Result.IGNORE;
        }

        if (mouseButton == 0) {
            int x = getContext().getMouseX() - getArea().x;
            int y = getContext().getMouseY() - getArea().y;
            Point pos = getCursorPosFromMouse(x, y);
            selStart.setLocation(pos);
            selEnd.setLocation(pos);
            hasSelection = false;
            isDragging = true;
            return Result.SUCCESS;
        }
        return Result.IGNORE;
    }

    @Override
    public void onMouseDrag(int mouseButton, long timeSinceClick) {
        if (isDragging && isFocused()) {
            int x = getContext().getMouseX() - getArea().x;
            int y = getContext().getMouseY() - getArea().y;
            Point pos = getCursorPosFromMouse(x, y);
            selEnd.setLocation(pos);
            hasSelection = !selStart.equals(selEnd);
        }
    }

    @Override
    public boolean onMouseRelease(int mouseButton) {
        isDragging = false;
        return true;
    }

    @Override
    public Result onKeyPressed(char character, int keyCode) {
        if (!isFocused()) {
            return Result.IGNORE;
        }

        if (Interactable.isKeyComboCtrlC(keyCode)) {
            String selected = getSelectedText();
            if (!selected.isEmpty()) {
                GuiScreen.setClipboardString(selected);
            }
            return Result.SUCCESS;
        }

        if (Interactable.isKeyComboCtrlA(keyCode)) {
            selectAll();
            return Result.SUCCESS;
        }

        return Result.IGNORE;
    }

    public void selectAll() {
        if (this.lines.isEmpty()) return;
        selStart.setLocation(0, 0);
        int lastLine = this.lines.size() - 1;
        selEnd.setLocation(
            this.lines.get(lastLine)
                .length(),
            lastLine);
        hasSelection = true;
    }

    private Point getSelectionStart() {
        if (selStart.y < selEnd.y || (selStart.y == selEnd.y && selStart.x <= selEnd.x)) {
            return selStart;
        }
        return selEnd;
    }

    private Point getSelectionEnd() {
        if (selStart.y < selEnd.y || (selStart.y == selEnd.y && selStart.x <= selEnd.x)) {
            return selEnd;
        }
        return selStart;
    }

    public String getSelectedText() {
        if (!hasSelection) return "";

        Point start = getSelectionStart();
        Point end = getSelectionEnd();

        if (start.y == end.y) {
            String line = this.lines.get(start.y);
            int s = Math.min(start.x, line.length());
            int e = Math.min(end.x, line.length());
            return line.substring(s, e);
        }

        StringBuilder sb = new StringBuilder();
        for (int i = start.y; i <= end.y && i < this.lines.size(); i++) {
            String line = this.lines.get(i);
            if (i == start.y) {
                sb.append(line.substring(Math.min(start.x, line.length())));
            } else if (i == end.y) {
                sb.append("\n")
                    .append(line.substring(0, Math.min(end.x, line.length())));
            } else {
                sb.append("\n")
                    .append(line);
            }
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
        hasSelection = false;
        isDragging = false;
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
