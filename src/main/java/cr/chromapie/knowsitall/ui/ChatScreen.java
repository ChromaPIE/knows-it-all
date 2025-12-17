package cr.chromapie.knowsitall.ui;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.factory.ClientGUI;
import com.cleanroommc.modularui.screen.CustomModularScreen;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.utils.Color;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.layout.Row;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import cr.chromapie.knowsitall.KnowsItAll;
import cr.chromapie.knowsitall.api.ChatMessage;
import cr.chromapie.knowsitall.network.ChatRequestPacket;
import cr.chromapie.knowsitall.network.ChatResponsePacket;
import cr.chromapie.knowsitall.network.ChatSyncRequestPacket;
import cr.chromapie.knowsitall.network.PacketHandler;

@SideOnly(Side.CLIENT)
public class ChatScreen extends CustomModularScreen {

    private static final int WIDTH = 320;
    private static final int HEIGHT = 220;

    private static final int PANEL_BG = Color.argb(30, 30, 35, 240);
    private static final int MSG_AREA_BG = Color.argb(20, 20, 25, 200);
    private static final int USER_MSG_BG = Color.argb(50, 70, 100, 230);
    private static final int AI_MSG_BG = Color.argb(70, 50, 90, 230);
    private static final int INPUT_BG = Color.argb(40, 40, 50, 200);

    private static final List<DisplayMessage> messages = new ArrayList<>();
    private ListWidget<Widget<?>, ?> messageList;
    private MultiLineInput inputField;
    private TextWidget emptyPlaceholder;

    public ChatScreen() {
        super(KnowsItAll.MODID);
    }

    @Override
    @NotNull
    public ModularPanel buildUI(ModularGuiContext context) {
        ModularPanel panel = ModularPanel.defaultPanel("knows_chat", WIDTH, HEIGHT);
        panel.background(new Rectangle().setColor(PANEL_BG));

        messageList = new ListWidget<>();
        messageList.left(6)
            .right(6)
            .top(6)
            .bottom(50);
        messageList.background(new Rectangle().setColor(MSG_AREA_BG));
        messageList.padding(4);

        emptyPlaceholder = new TextWidget(IKey.str("§7§l§oWhat can I help you with today?"));
        emptyPlaceholder.left(6)
            .right(6)
            .top(6)
            .bottom(50);
        emptyPlaceholder.alignment(Alignment.Center);

        inputField = new MultiLineInput();
        inputField.height(38);
        inputField.expanded();
        inputField.setMaxLength(500);
        inputField.background(new Rectangle().setColor(INPUT_BG));
        inputField.onEnter(text -> sendMessage());

        ButtonWidget<?> sendBtn = new ButtonWidget<>();
        sendBtn.size(40, 38);
        sendBtn.overlay(IKey.str("§aSend"));
        sendBtn.onMousePressed(btn -> {
            sendMessage();
            return true;
        });

        Row inputRow = new Row();
        inputRow.left(6)
            .right(6)
            .bottom(6)
            .height(38);
        inputRow.child(inputField.marginRight(4))
            .child(sendBtn);

        panel.child(messageList);
        panel.child(emptyPlaceholder);
        panel.child(inputRow);

        for (DisplayMessage msg : messages) {
            messageList.child(createMessageWidget(msg));
        }

        updatePlaceholderVisibility();

        return panel;
    }

    private static ChatScreen instance;

    private void sendMessage() {
        if (inputField == null) return;
        String text = inputField.getText()
            .trim();
        if (text.isEmpty()) return;

        addMessage(new DisplayMessage(DisplayMessage.Role.USER, text));
        inputField.clear();
        PacketHandler.INSTANCE.sendToServer(new ChatRequestPacket(text));
    }

    public static void receiveResponse(String content, int messageType) {
        if (instance != null) {
            DisplayMessage.Role role;
            String formattedContent = switch (messageType) {
                case ChatResponsePacket.TYPE_ERROR -> {
                    role = DisplayMessage.Role.SYSTEM;
                    yield "§c" + content;
                }
                case ChatResponsePacket.TYPE_TOOL_HINT -> {
                    role = DisplayMessage.Role.TOOL_HINT;
                    yield "§7§o" + content;
                }
                default -> {
                    role = DisplayMessage.Role.ASSISTANT;
                    yield content;
                }
            };
            instance.addMessage(new DisplayMessage(role, formattedContent));
        }
    }

    public void addMessage(DisplayMessage msg) {
        messages.add(msg);
        if (messageList != null && messageList.isValid()) {
            boolean wasAtBottom = isScrolledToBottom();
            messageList.child(createMessageWidget(msg));
            updatePlaceholderVisibility();
            if (wasAtBottom) {
                scrollToBottom();
            }
        }
    }

    private boolean isScrolledToBottom() {
        if (messageList == null || !messageList.isValid()) return true;
        var scrollData = messageList.getScrollData();
        var scrollArea = messageList.getScrollArea();
        if (scrollData == null || scrollArea == null) return true;
        int scroll = scrollData.getScroll();
        int scrollSize = scrollData.getScrollSize();
        int visibleSize = scrollData.getFullVisibleSize(scrollArea);
        int maxScroll = Math.max(0, scrollSize - visibleSize);
        return scroll >= maxScroll - 5;
    }

    private void scrollToBottom() {
        if (messageList == null || !messageList.isValid()) return;
        var scrollData = messageList.getScrollData();
        var scrollArea = messageList.getScrollArea();
        if (scrollData == null || scrollArea == null) return;
        int scrollSize = scrollData.getScrollSize();
        int visibleSize = scrollData.getFullVisibleSize(scrollArea);
        int targetScroll = Math.max(0, scrollSize - visibleSize);
        scrollData.scrollTo(scrollArea, targetScroll);
    }

    private void updatePlaceholderVisibility() {
        if (emptyPlaceholder != null) {
            emptyPlaceholder.setEnabled(messages.isEmpty());
        }
    }

    private static final int LABEL_WIDTH = 42;

    private Widget<?> createMessageWidget(DisplayMessage msg) {
        boolean isUser = msg.role == DisplayMessage.Role.USER;
        boolean isError = msg.role == DisplayMessage.Role.SYSTEM;
        boolean isToolHint = msg.role == DisplayMessage.Role.TOOL_HINT;

        if (isToolHint) {
            TextWidget spacer = new TextWidget(IKey.str(""));
            spacer.width(LABEL_WIDTH);

            SelectableTextWidget contentWidget = new SelectableTextWidget(IKey.str(msg.content));
            contentWidget.expanded();
            contentWidget.siblingWidth(LABEL_WIDTH);
            contentWidget.alignment(Alignment.TopLeft);
            contentWidget.padding(3)
                .paddingBottom(2);

            Flow container = Flow.row()
                .crossAxisAlignment(Alignment.CrossAxis.START)
                .widthRel(1f)
                .coverChildrenHeight();
            container.child(spacer);
            container.child(contentWidget);
            return container;
        }

        int bgColor = isUser ? USER_MSG_BG : AI_MSG_BG;
        String label = isUser ? "§7You:" : (isError ? "§c✖" : "§dKNOWS:");

        TextWidget labelWidget = new TextWidget(IKey.str(label));
        labelWidget.width(LABEL_WIDTH)
            .top(3);

        SelectableTextWidget contentWidget = new SelectableTextWidget(IKey.str(ensureWhiteLines(msg.content)));
        contentWidget.expanded();
        contentWidget.siblingWidth(LABEL_WIDTH);
        contentWidget.alignment(Alignment.TopLeft);
        contentWidget.background(new Rectangle().setColor(bgColor));
        contentWidget.padding(3)
            .paddingBottom(5);

        Flow container = Flow.row()
            .crossAxisAlignment(Alignment.CrossAxis.START)
            .widthRel(1f)
            .coverChildrenHeight()
            .marginBottom(2);
        container.child(labelWidget);
        container.child(contentWidget);

        return container;
    }

    private String ensureWhiteLines(String text) {
        if (text == null || text.isEmpty()) return text;
        StringBuilder sb = new StringBuilder();
        String[] lines = text.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) sb.append("\n");
            String line = lines[i];
            if (!line.startsWith("§")) {
                sb.append("§f");
            }
            sb.append(line);
        }
        return sb.toString();
    }

    @Override
    public void onOpen() {
        super.onOpen();
        instance = this;
        PacketHandler.INSTANCE.sendToServer(new ChatSyncRequestPacket());
    }

    @Override
    public void onClose() {
        super.onClose();
        instance = null;
    }

    @SideOnly(Side.CLIENT)
    public static void open() {
        ClientGUI.open(new ChatScreen());
    }

    public static void clearMessages() {
        messages.clear();
        if (instance != null && instance.messageList != null) {
            instance.messageList.getChildren()
                .clear();
            instance.updatePlaceholderVisibility();
        }
    }

    public static void syncMessages(List<ChatMessage> history) {
        messages.clear();
        for (ChatMessage msg : history) {
            if (!msg.isDisplayable()) {
                continue;
            }
            DisplayMessage.Role role;
            if (msg.isToolHint()) {
                role = DisplayMessage.Role.TOOL_HINT;
            } else if ("user".equals(msg.getRole())) {
                role = DisplayMessage.Role.USER;
            } else {
                role = DisplayMessage.Role.ASSISTANT;
            }
            String content = msg.isToolHint() ? "§7§o" + msg.getContent() : msg.getContent();
            messages.add(new DisplayMessage(role, content));
        }
        if (instance != null && instance.messageList != null) {
            instance.messageList.getChildren()
                .clear();
            for (DisplayMessage msg : messages) {
                instance.messageList.child(instance.createMessageWidget(msg));
            }
            instance.updatePlaceholderVisibility();
            instance.scrollToBottom();
        }
    }

    public static class DisplayMessage {

        public enum Role {
            USER,
            ASSISTANT,
            SYSTEM,
            TOOL_HINT
        }

        public final Role role;
        public final String content;

        public DisplayMessage(Role role, String content) {
            this.role = role;
            this.content = content;
        }
    }
}
