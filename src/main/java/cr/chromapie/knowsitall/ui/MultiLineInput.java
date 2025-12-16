package cr.chromapie.knowsitall.ui;

import com.cleanroommc.modularui.api.UpOrDown;
import com.cleanroommc.modularui.api.widget.IFocusedWidget;
import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.utils.Color;
import com.cleanroommc.modularui.widget.Widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.input.Keyboard;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@SideOnly(Side.CLIENT)
public class MultiLineInput extends Widget<MultiLineInput> implements Interactable, IFocusedWidget {

    private final StringBuilder text = new StringBuilder();
    private int cursorPos = 0;
    private int selectionStart = -1;
    private int scrollOffset = 0;
    private int maxLength = 500;
    private int textColor = Color.WHITE.main;
    private int cursorColor = Color.WHITE.main;
    private int selectionColor = Color.argb(80, 100, 150, 180);
    private Consumer<String> onEnter;
    private long cursorBlinkTime = 0;
    private boolean focused = false;
    private List<String> cachedLines = new ArrayList<>();
    private int cachedWidth = 0;
    private int lastCursorPos = -1;

    public MultiLineInput() {
        padding(4);
    }

    public MultiLineInput setMaxLength(int maxLength) {
        this.maxLength = maxLength;
        return this;
    }

    public MultiLineInput setTextColor(int color) {
        this.textColor = color;
        return this;
    }

    public MultiLineInput onEnter(Consumer<String> callback) {
        this.onEnter = callback;
        return this;
    }

    public String getText() {
        return text.toString();
    }

    public void setText(String newText) {
        text.setLength(0);
        if (newText != null) {
            text.append(newText.length() > maxLength ? newText.substring(0, maxLength) : newText);
        }
        cursorPos = text.length();
    }

    public void clear() {
        text.setLength(0);
        cursorPos = 0;
        selectionStart = -1;
        scrollOffset = 0;
        invalidateCache();
    }

    private void updateCache(int width, FontRenderer font) {
        if (width != cachedWidth) {
            cachedWidth = width;
            cachedLines = wrapText(text.toString(), width, font);
        }
    }

    private void invalidateCache() {
        cachedWidth = 0;
        cachedLines.clear();
        cachedLines.add("");
    }

    private int[] getCursorLineCol(int pos) {
        int charCount = 0;
        for (int i = 0; i < cachedLines.size(); i++) {
            int lineLen = cachedLines.get(i).length();
            if (charCount + lineLen >= pos) {
                return new int[]{i, pos - charCount};
            }
            charCount += lineLen;
        }
        return new int[]{cachedLines.size() - 1, cachedLines.isEmpty() ? 0 : cachedLines.get(cachedLines.size() - 1).length()};
    }

    private int getPosFromLineCol(int line, int col) {
        int pos = 0;
        for (int i = 0; i < line && i < cachedLines.size(); i++) {
            pos += cachedLines.get(i).length();
        }
        if (line < cachedLines.size()) {
            pos += Math.min(col, cachedLines.get(line).length());
        }
        return Math.min(pos, text.length());
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
        super.draw(context, widgetTheme);

        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        int areaWidth = getArea().width - getArea().getPadding().horizontal();
        int areaHeight = getArea().height - getArea().getPadding().vertical();
        int padLeft = getArea().getPadding().getLeft();
        int padTop = getArea().getPadding().getTop();

        updateCache(areaWidth, font);
        int lineHeight = font.FONT_HEIGHT + 1;
        int maxVisibleLines = areaHeight / lineHeight;

        int[] cursorLC = getCursorLineCol(cursorPos);
        int cursorLine = cursorLC[0], cursorCol = cursorLC[1];

        if (cursorPos != lastCursorPos) {
            lastCursorPos = cursorPos;
            if (cursorLine < scrollOffset) {
                scrollOffset = cursorLine;
            } else if (cursorLine >= scrollOffset + maxVisibleLines) {
                scrollOffset = cursorLine - maxVisibleLines + 1;
            }
        }

        int selStart = selectionStart >= 0 ? Math.min(selectionStart, cursorPos) : -1;
        int selEnd = selectionStart >= 0 ? Math.max(selectionStart, cursorPos) : -1;

        for (int i = 0; i < maxVisibleLines && i + scrollOffset < cachedLines.size(); i++) {
            int lineIdx = i + scrollOffset;
            String line = cachedLines.get(lineIdx);
            int y = padTop + i * lineHeight;

            if (selStart >= 0) {
                int lineStart = getPosFromLineCol(lineIdx, 0);
                int lineEnd = lineStart + line.length();
                if (selEnd > lineStart && selStart < lineEnd) {
                    int s = Math.max(0, selStart - lineStart);
                    int e = Math.min(line.length(), selEnd - lineStart);
                    int x1 = padLeft + font.getStringWidth(line.substring(0, s));
                    int x2 = padLeft + font.getStringWidth(line.substring(0, e));
                    GuiDraw.drawRect(x1, y, x2 - x1, lineHeight, selectionColor);
                }
            }

            font.drawString(line, padLeft, y, textColor);
        }

        if (isFocused() && (System.currentTimeMillis() - cursorBlinkTime) % 1000 < 500) {
            int displayLine = cursorLine - scrollOffset;
            if (displayLine >= 0 && displayLine < maxVisibleLines) {
                String lineText = cursorLine < cachedLines.size() ? cachedLines.get(cursorLine) : "";
                int cursorX = padLeft + font.getStringWidth(lineText.substring(0, Math.min(cursorCol, lineText.length())));
                GuiDraw.drawRect(cursorX, padTop + displayLine * lineHeight, 1, lineHeight - 1, cursorColor);
            }
        }
    }

    private List<String> wrapText(String text, int maxWidth, FontRenderer font) {
        List<String> lines = new ArrayList<>();
        if (text.isEmpty()) {
            lines.add("");
            return lines;
        }

        StringBuilder currentLine = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            currentLine.append(c);
            if (font.getStringWidth(currentLine.toString()) > maxWidth) {
                if (currentLine.length() > 1) {
                    lines.add(currentLine.substring(0, currentLine.length() - 1));
                    currentLine = new StringBuilder().append(c);
                } else {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder();
                }
            }
        }
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }
        return lines;
    }

    @Override
    public @NotNull Result onKeyPressed(char character, int keyCode) {
        if (!isFocused()) return Result.IGNORE;
        boolean shift = Interactable.hasShiftDown();

        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
            if (onEnter != null) onEnter.accept(text.toString());
            return Result.SUCCESS;
        }

        if (keyCode == Keyboard.KEY_BACK) {
            if (hasSelection()) {
                deleteSelection();
            } else if (cursorPos > 0) {
                text.deleteCharAt(--cursorPos);
                invalidateCache();
            }
            return Result.SUCCESS;
        }

        if (keyCode == Keyboard.KEY_DELETE) {
            if (hasSelection()) {
                deleteSelection();
            } else if (cursorPos < text.length()) {
                text.deleteCharAt(cursorPos);
                invalidateCache();
            }
            return Result.SUCCESS;
        }

        if (keyCode == Keyboard.KEY_LEFT) {
            if (!shift && hasSelection()) {
                cursorPos = Math.min(selectionStart, cursorPos);
                selectionStart = -1;
            } else {
                if (shift && selectionStart < 0) selectionStart = cursorPos;
                if (cursorPos > 0) cursorPos--;
                if (!shift) selectionStart = -1;
            }
            return Result.SUCCESS;
        }

        if (keyCode == Keyboard.KEY_RIGHT) {
            if (!shift && hasSelection()) {
                cursorPos = Math.max(selectionStart, cursorPos);
                selectionStart = -1;
            } else {
                if (shift && selectionStart < 0) selectionStart = cursorPos;
                if (cursorPos < text.length()) cursorPos++;
                if (!shift) selectionStart = -1;
            }
            return Result.SUCCESS;
        }

        if (keyCode == Keyboard.KEY_UP) {
            if (shift && selectionStart < 0) selectionStart = cursorPos;
            int[] lc = getCursorLineCol(cursorPos);
            if (lc[0] > 0) cursorPos = getPosFromLineCol(lc[0] - 1, lc[1]);
            if (!shift) selectionStart = -1;
            return Result.SUCCESS;
        }

        if (keyCode == Keyboard.KEY_DOWN) {
            if (shift && selectionStart < 0) selectionStart = cursorPos;
            int[] lc = getCursorLineCol(cursorPos);
            if (lc[0] < cachedLines.size() - 1) cursorPos = getPosFromLineCol(lc[0] + 1, lc[1]);
            if (!shift) selectionStart = -1;
            return Result.SUCCESS;
        }

        if (keyCode == Keyboard.KEY_HOME) {
            if (shift && selectionStart < 0) selectionStart = cursorPos;
            cursorPos = 0;
            if (!shift) selectionStart = -1;
            return Result.SUCCESS;
        }

        if (keyCode == Keyboard.KEY_END) {
            if (shift && selectionStart < 0) selectionStart = cursorPos;
            cursorPos = text.length();
            if (!shift) selectionStart = -1;
            return Result.SUCCESS;
        }

        if (Interactable.isKeyComboCtrlA(keyCode)) {
            selectionStart = 0;
            cursorPos = text.length();
            return Result.SUCCESS;
        }

        if (Interactable.isKeyComboCtrlC(keyCode)) {
            if (hasSelection()) GuiScreen.setClipboardString(getSelectedText());
            return Result.SUCCESS;
        }

        if (Interactable.isKeyComboCtrlX(keyCode)) {
            if (hasSelection()) {
                GuiScreen.setClipboardString(getSelectedText());
                deleteSelection();
            }
            return Result.SUCCESS;
        }

        if (Interactable.isKeyComboCtrlV(keyCode)) {
            String clipboard = GuiScreen.getClipboardString();
            if (clipboard != null) {
                if (hasSelection()) deleteSelection();
                insertText(clipboard.replace("\n", " ").replace("\r", ""));
            }
            return Result.SUCCESS;
        }

        if (character >= 32 && character != 167) {
            if (hasSelection()) deleteSelection();
            insertText(String.valueOf(character));
            return Result.SUCCESS;
        }

        return Result.ACCEPT;
    }

    private boolean hasSelection() {
        return selectionStart >= 0 && selectionStart != cursorPos;
    }

    private String getSelectedText() {
        int start = Math.min(selectionStart, cursorPos);
        int end = Math.max(selectionStart, cursorPos);
        return text.substring(start, end);
    }

    private void deleteSelection() {
        int start = Math.min(selectionStart, cursorPos);
        int end = Math.max(selectionStart, cursorPos);
        text.delete(start, end);
        cursorPos = start;
        selectionStart = -1;
        invalidateCache();
    }

    private void insertText(String str) {
        int available = maxLength - text.length();
        if (available <= 0) return;
        String toInsert = str.length() > available ? str.substring(0, available) : str;
        text.insert(cursorPos, toInsert);
        cursorPos += toInsert.length();
        invalidateCache();
    }

    @Override
    public @NotNull Result onMousePressed(int mouseButton) {
        if (isHovering()) {
            if (!isFocused()) getContext().focus(this);
            cursorBlinkTime = System.currentTimeMillis();
            cursorPos = getPositionFromMouse();
            selectionStart = cursorPos;
            return Result.SUCCESS;
        }
        return Result.IGNORE;
    }

    @Override
    public boolean onMouseRelease(int mouseButton) {
        if (selectionStart == cursorPos) selectionStart = -1;
        return true;
    }

    @Override
    public void onMouseDrag(int mouseButton, long timeSinceClick) {
        if (isFocused()) {
            cursorPos = getPositionFromMouse();
            cursorBlinkTime = System.currentTimeMillis();
        }
    }

    @Override
    public boolean onMouseScroll(UpOrDown scrollDirection, int amount) {
        if (isHovering() && cachedLines.size() > 1) {
            scrollOffset = Math.max(0, Math.min(scrollOffset + (scrollDirection == UpOrDown.UP ? -1 : 1), cachedLines.size() - 1));
            return true;
        }
        return false;
    }

    private int getPositionFromMouse() {
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        int areaWidth = getArea().width - getArea().getPadding().horizontal();
        int padLeft = getArea().getPadding().getLeft();
        int padTop = getArea().getPadding().getTop();
        int lineHeight = font.FONT_HEIGHT + 1;

        updateCache(areaWidth, font);

        int mx = getContext().getMouseX() - padLeft;
        int my = getContext().getMouseY() - padTop;
        int lineIdx = Math.max(0, Math.min(scrollOffset + my / lineHeight, cachedLines.size() - 1));

        if (cachedLines.isEmpty()) return 0;

        String line = cachedLines.get(lineIdx);
        int col = 0;
        for (int i = 0; i <= line.length(); i++) {
            if (font.getStringWidth(line.substring(0, i)) >= mx) {
                col = i > 0 && mx < font.getStringWidth(line.substring(0, i)) - font.getCharWidth(line.charAt(i - 1)) / 2 ? i - 1 : i;
                break;
            }
            col = i;
        }
        return getPosFromLineCol(lineIdx, col);
    }

    @Override
    public void onFocus(ModularGuiContext context) {
        focused = true;
        cursorBlinkTime = System.currentTimeMillis();
    }

    @Override
    public void onRemoveFocus(ModularGuiContext context) {
        focused = false;
    }

    @Override
    public boolean isFocused() {
        return focused;
    }

    @Override
    public boolean canHover() {
        return true;
    }
}