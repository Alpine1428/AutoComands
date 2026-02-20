
package com.commandspammer.gui;

import com.commandspammer.CommandSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class CommandSpammerScreen extends Screen {

    private TextFieldWidget delayField;
    private TextFieldWidget[] commandLines;
    private static final int MAX_LINES = 30;
    private static final int VISIBLE_LINES = 8;

    private int activeLines = 5;
    private int scrollOffset = 0;

    // Сохранение состояния между открытиями
    private static String[] savedLines = new String[MAX_LINES];
    private static String savedDelay = "20";
    private static int savedActiveLines = 5;

    public CommandSpammerScreen() {
        super(Text.literal("Command Spammer"));
    }

    @Override
    protected void init() {
        super.init();
        activeLines = savedActiveLines;

        int cx = this.width / 2;
        int sy = 25;

        // ── Поле задержки ──
        delayField = new TextFieldWidget(
                this.textRenderer, cx - 60, sy + 18, 120, 16,
                Text.literal("Delay"));
        delayField.setMaxLength(10);
        delayField.setText(savedDelay);
        delayField.setPlaceholder(Text.literal("20 = 1 sec"));
        this.addDrawableChild(delayField);

        // ── Поля команд ──
        commandLines = new TextFieldWidget[MAX_LINES];
        int fieldsY = sy + 52;

        for (int i = 0; i < MAX_LINES; i++) {
            commandLines[i] = new TextFieldWidget(
                    this.textRenderer, cx - 152, fieldsY, 304, 16,
                    Text.literal("cmd" + i));
            commandLines[i].setMaxLength(256);
            if (savedLines[i] != null) {
                commandLines[i].setText(savedLines[i]);
            }
            commandLines[i].setPlaceholder(Text.literal("/command " + (i + 1)));
            commandLines[i].visible = false;
        }

        refreshVisibleFields(fieldsY);

        // ── Кнопки ──
        int btnY = fieldsY + Math.min(activeLines, VISIBLE_LINES) * 20 + 8;

        // + / - строки
        this.addDrawableChild(ButtonWidget.builder(Text.literal("+"), b -> {
            if (activeLines < MAX_LINES) { activeLines++; savedActiveLines = activeLines; rebuild(); }
        }).dimensions(cx - 152, btnY, 30, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("-"), b -> {
            if (activeLines > 1) {
                commandLines[activeLines - 1].setText("");
                activeLines--; savedActiveLines = activeLines; rebuild();
            }
        }).dimensions(cx - 118, btnY, 30, 20).build());

        // Вставить из буфера
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Paste Clipboard"), b -> {
            pasteClipboard();
        }).dimensions(cx - 82, btnY, 110, 20).build());

        // Очистить
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Clear"), b -> {
            for (int i = 0; i < MAX_LINES; i++) {
                commandLines[i].setText("");
                savedLines[i] = null;
            }
            delayField.setText("20"); savedDelay = "20";
        }).dimensions(cx + 32, btnY, 60, 20).build());

        // Scroll up / down
        this.addDrawableChild(ButtonWidget.builder(Text.literal("^"), b -> {
            if (scrollOffset > 0) { scrollOffset--; rebuild(); }
        }).dimensions(cx + 96, btnY, 25, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("v"), b -> {
            if (scrollOffset < activeLines - VISIBLE_LINES) { scrollOffset++; rebuild(); }
        }).dimensions(cx + 125, btnY, 25, 20).build());

        btnY += 26;

        // ── START / PAUSE / STOP ──
        this.addDrawableChild(ButtonWidget.builder(Text.literal(">> START"), b -> {
            saveState();
            CommandSender sender = CommandSender.getInstance();
            try {
                sender.setDelayTicks(Integer.parseInt(delayField.getText().trim()));
            } catch (NumberFormatException e) {
                sender.setDelayTicks(20);
            }
            List<String> cmds = new ArrayList<>();
            for (int i = 0; i < activeLines; i++) {
                String t = commandLines[i].getText().trim();
                if (!t.isEmpty()) cmds.add(t);
            }
            sender.setCommands(cmds);
            sender.start();
            this.close();
        }).dimensions(cx - 152, btnY, 100, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("|| PAUSE"), b -> {
            CommandSender.getInstance().togglePause();
        }).dimensions(cx - 48, btnY, 100, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("[] STOP"), b -> {
            CommandSender.getInstance().stop();
        }).dimensions(cx + 56, btnY, 96, 20).build());
    }

    private void refreshVisibleFields(int fieldsY) {
        for (int i = 0; i < MAX_LINES; i++) {
            if (i >= scrollOffset && i < scrollOffset + VISIBLE_LINES && i < activeLines) {
                int vi = i - scrollOffset;
                commandLines[i].setX(this.width / 2 - 152);
                commandLines[i].setY(fieldsY + vi * 20);
                commandLines[i].visible = true;
                this.addDrawableChild(commandLines[i]);
            } else {
                commandLines[i].visible = false;
            }
        }
    }

    private void pasteClipboard() {
        try {
            String clip = MinecraftClient.getInstance().keyboard.getClipboard();
            if (clip == null || clip.isEmpty()) return;
            String[] lines = clip.split("\\r?\\n");
            if (lines.length > activeLines) {
                activeLines = Math.min(lines.length, MAX_LINES);
                savedActiveLines = activeLines;
            }
            for (int i = 0; i < Math.min(lines.length, MAX_LINES); i++) {
                commandLines[i].setText(lines[i].trim());
            }
            rebuild();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void rebuild() {
        saveState();
        this.clearChildren();
        this.init();
    }

    private void saveState() {
        for (int i = 0; i < MAX_LINES; i++) {
            if (commandLines != null && commandLines[i] != null)
                savedLines[i] = commandLines[i].getText();
        }
        if (delayField != null) savedDelay = delayField.getText();
    }

    @Override
    public void close() {
        saveState();
        super.close();
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double amt) {
        if (activeLines > VISIBLE_LINES) {
            scrollOffset -= (int) amt;
            scrollOffset = Math.max(0, Math.min(scrollOffset, activeLines - VISIBLE_LINES));
            rebuild();
            return true;
        }
        return super.mouseScrolled(mx, my, amt);
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        this.renderBackground(ctx);
        super.render(ctx, mx, my, delta);

        int cx = this.width / 2;

        // Заголовок
        ctx.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("\u00a7b\u00a7l=== Command Spammer ==="), cx, 8, 0xFFFFFF);

        // Метка задержки
        ctx.drawTextWithShadow(this.textRenderer,
                Text.literal("\u00a7eDelay (ticks):"), cx - 60, 27, 0xFFFFFF);

        // Метка команд
        ctx.drawTextWithShadow(this.textRenderer,
                Text.literal("\u00a7aCommands:"), cx - 152, 47, 0xFFFFFF);

        // Нумерация строк
        int fieldsY = 77;
        for (int i = scrollOffset; i < Math.min(activeLines, scrollOffset + VISIBLE_LINES); i++) {
            int vi = i - scrollOffset;
            ctx.drawTextWithShadow(this.textRenderer,
                    Text.literal("\u00a77" + (i + 1) + "."),
                    cx - 172, fieldsY + vi * 20 + 4, 0x888888);
        }

        // Статус
        CommandSender s = CommandSender.getInstance();
        String status;
        int color;
        if (s.isRunning()) {
            if (s.isPaused()) {
                status = "PAUSED"; color = 0xFFFF00;
            } else {
                status = "RUNNING (" + s.getCurrentIndex() + "/" + s.getTotalCommands() + ")";
                color = 0x00FF00;
            }
        } else {
            status = "STOPPED"; color = 0xFF4444;
        }
        ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal(status), cx, this.height - 18, color);
        ctx.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("\u00a78Press P to open/close | Lines: " + activeLines),
                cx, this.height - 8, 0x666666);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
