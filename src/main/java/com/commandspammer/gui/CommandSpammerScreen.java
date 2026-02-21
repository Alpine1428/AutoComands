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

    // Динамический список полей — без лимита
    private List<TextFieldWidget> commandFields = new ArrayList<>();
    private int scrollOffset = 0;
    private static final int VISIBLE_LINES = 10;

    // Сохранение между открытиями
    private static List<String> savedCommands = new ArrayList<>();
    private static String savedDelay = "20";

    public CommandSpammerScreen() {
        super(Text.literal("Command Spammer"));
    }

    @Override
    protected void init() {
        super.init();

        // Если первый запуск — добавляем 5 пустых строк
        if (savedCommands.isEmpty()) {
            for (int i = 0; i < 5; i++) {
                savedCommands.add("");
            }
        }

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
        commandFields.clear();
        int fieldsY = sy + 52;

        for (int i = 0; i < savedCommands.size(); i++) {
            TextFieldWidget field = new TextFieldWidget(
                    this.textRenderer, cx - 152, fieldsY, 304, 16,
                    Text.literal("cmd" + i));
            field.setMaxLength(256);
            field.setText(savedCommands.get(i));
            field.setPlaceholder(Text.literal("/command " + (i + 1)));
            field.visible = false;
            commandFields.add(field);
        }

        refreshVisibleFields(fieldsY);

        // ── Кнопки ──
        int btnY = fieldsY + VISIBLE_LINES * 20 + 8;

        // + добавить строку
        this.addDrawableChild(ButtonWidget.builder(Text.literal("+ Add Line"), b -> {
            savedCommands.add("");
            rebuild();
        }).dimensions(cx - 152, btnY, 80, 20).build());

        // +10 строк
        this.addDrawableChild(ButtonWidget.builder(Text.literal("+10"), b -> {
            for (int i = 0; i < 10; i++) savedCommands.add("");
            rebuild();
        }).dimensions(cx - 68, btnY, 35, 20).build());

        // +100 строк
        this.addDrawableChild(ButtonWidget.builder(Text.literal("+100"), b -> {
            for (int i = 0; i < 100; i++) savedCommands.add("");
            rebuild();
        }).dimensions(cx - 29, btnY, 42, 20).build());

        // - убрать строку
        this.addDrawableChild(ButtonWidget.builder(Text.literal("-"), b -> {
            if (savedCommands.size() > 1) {
                savedCommands.remove(savedCommands.size() - 1);
                if (scrollOffset > Math.max(0, savedCommands.size() - VISIBLE_LINES)) {
                    scrollOffset = Math.max(0, savedCommands.size() - VISIBLE_LINES);
                }
                rebuild();
            }
        }).dimensions(cx + 17, btnY, 25, 20).build());

        // Вставить из буфера
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Paste"), b -> {
            pasteClipboard();
        }).dimensions(cx + 46, btnY, 50, 20).build());

        // Очистить
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Clear"), b -> {
            savedCommands.clear();
            for (int i = 0; i < 5; i++) savedCommands.add("");
            scrollOffset = 0;
            delayField.setText("20");
            savedDelay = "20";
            rebuild();
        }).dimensions(cx + 100, btnY, 52, 20).build());

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
            for (String s : savedCommands) {
                String t = s.trim();
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
        for (int i = 0; i < commandFields.size(); i++) {
            TextFieldWidget field = commandFields.get(i);
            if (i >= scrollOffset && i < scrollOffset + VISIBLE_LINES) {
                int vi = i - scrollOffset;
                field.setX(this.width / 2 - 152);
                field.setY(fieldsY + vi * 20);
                field.visible = true;
                this.addDrawableChild(field);
            } else {
                field.visible = false;
            }
        }
    }

    private void pasteClipboard() {
        try {
            String clip = MinecraftClient.getInstance().keyboard.getClipboard();
            if (clip == null || clip.isEmpty()) return;
            String[] lines = clip.split("\\r?\\n");

            saveState();
            savedCommands.clear();

            for (String line : lines) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    savedCommands.add(trimmed);
                }
            }

            // Минимум 1 строка
            if (savedCommands.isEmpty()) {
                savedCommands.add("");
            }

            scrollOffset = 0;
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
        if (commandFields != null) {
            for (int i = 0; i < commandFields.size(); i++) {
                if (i < savedCommands.size()) {
                    savedCommands.set(i, commandFields.get(i).getText());
                }
            }
        }
        if (delayField != null) {
            savedDelay = delayField.getText();
        }
    }

    @Override
    public void close() {
        saveState();
        super.close();
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double amt) {
        int maxScroll = Math.max(0, savedCommands.size() - VISIBLE_LINES);
        scrollOffset -= (int) amt;
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
        rebuild();
        return true;
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
        int totalLines = savedCommands.size();
        long filledLines = savedCommands.stream().filter(s -> !s.trim().isEmpty()).count();
        ctx.drawTextWithShadow(this.textRenderer,
                Text.literal("\u00a7aCommands (" + filledLines + " filled / " + totalLines + " total):"),
                cx - 152, 47, 0xFFFFFF);

        // Нумерация строк
        int fieldsY = 77;
        for (int i = scrollOffset; i < Math.min(savedCommands.size(), scrollOffset + VISIBLE_LINES); i++) {
            int vi = i - scrollOffset;
            ctx.drawTextWithShadow(this.textRenderer,
                    Text.literal("\u00a77" + (i + 1) + "."),
                    cx - 172, fieldsY + vi * 20 + 4, 0x888888);
        }

        // Скроллбар
        if (savedCommands.size() > VISIBLE_LINES) {
            int barX = cx + 156;
            int barTopY = fieldsY;
            int barHeight = VISIBLE_LINES * 20;
            int maxScroll = savedCommands.size() - VISIBLE_LINES;

            // Фон скроллбара
            ctx.fill(barX, barTopY, barX + 4, barTopY + barHeight, 0x44FFFFFF);

            // Ползунок
            int thumbHeight = Math.max(10, barHeight * VISIBLE_LINES / savedCommands.size());
            int thumbY = barTopY + (maxScroll > 0 ? (barHeight - thumbHeight) * scrollOffset / maxScroll : 0);
            ctx.fill(barX, thumbY, barX + 4, thumbY + thumbHeight, 0xAAFFFFFF);
        }

        // Статус
        CommandSender s = CommandSender.getInstance();
        String status;
        int color;
        if (s.isRunning()) {
            if (s.isPaused()) {
                status = "PAUSED";
                color = 0xFFFF00;
            } else {
                status = "RUNNING (" + s.getCurrentIndex() + "/" + s.getTotalCommands() + ")";
                color = 0x00FF00;
            }
        } else {
            status = "STOPPED";
            color = 0xFF4444;
        }
        ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal(status), cx, this.height - 18, color);

        // Подсказки
        ctx.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("\u00a78P = menu | Scroll = navigate | Lines: " + totalLines),
                cx, this.height - 8, 0x666666);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
