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

    // ВСЕ поля — создаются для каждой строки, не только видимые
    private List<TextFieldWidget> allFields = new ArrayList<>();
    private int scrollOffset = 0;
    private static final int VISIBLE_LINES = 10;

    // Сохранение между открытиями — без лимита
    private static List<String> savedCommands = new ArrayList<>();
    private static String savedDelay = "20";

    public CommandSpammerScreen() {
        super(Text.literal("Command Spammer"));
    }

    @Override
    protected void init() {
        super.init();

        if (savedCommands.isEmpty()) {
            for (int i = 0; i < 5; i++) {
                savedCommands.add("");
            }
        }

        // Ограничиваем скролл
        int maxScroll = Math.max(0, savedCommands.size() - VISIBLE_LINES);
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;
        if (scrollOffset < 0) scrollOffset = 0;

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

        // ── Создаём ВСЕ поля для ВСЕХ команд ──
        allFields.clear();
        int fieldsY = sy + 52;

        for (int i = 0; i < savedCommands.size(); i++) {
            TextFieldWidget field = new TextFieldWidget(
                    this.textRenderer, cx - 152, 0, 304, 16,
                    Text.literal("cmd" + i));
            field.setMaxLength(256);
            field.setText(savedCommands.get(i));
            field.setPlaceholder(Text.literal("/command " + (i + 1)));
            field.visible = false;
            allFields.add(field);
        }

        // Показываем только видимые
        for (int i = 0; i < allFields.size(); i++) {
            TextFieldWidget field = allFields.get(i);
            if (i >= scrollOffset && i < scrollOffset + VISIBLE_LINES) {
                int vi = i - scrollOffset;
                field.setX(cx - 152);
                field.setY(fieldsY + vi * 20);
                field.visible = true;
                this.addDrawableChild(field);
            }
        }

        // ── Кнопки ──
        int btnY = fieldsY + VISIBLE_LINES * 20 + 8;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("+1"), b -> {
            saveAllFields();
            savedCommands.add("");
            rebuild();
        }).dimensions(cx - 152, btnY, 35, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("+10"), b -> {
            saveAllFields();
            for (int i = 0; i < 10; i++) savedCommands.add("");
            rebuild();
        }).dimensions(cx - 113, btnY, 40, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("+100"), b -> {
            saveAllFields();
            for (int i = 0; i < 100; i++) savedCommands.add("");
            rebuild();
        }).dimensions(cx - 69, btnY, 45, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("+1000"), b -> {
            saveAllFields();
            for (int i = 0; i < 1000; i++) savedCommands.add("");
            rebuild();
        }).dimensions(cx - 20, btnY, 50, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("-"), b -> {
            saveAllFields();
            if (savedCommands.size() > 1) {
                savedCommands.remove(savedCommands.size() - 1);
                int ms = Math.max(0, savedCommands.size() - VISIBLE_LINES);
                if (scrollOffset > ms) scrollOffset = ms;
                rebuild();
            }
        }).dimensions(cx + 34, btnY, 25, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Paste"), b -> {
            pasteClipboard();
        }).dimensions(cx + 63, btnY, 45, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Clear"), b -> {
            savedCommands.clear();
            for (int i = 0; i < 5; i++) savedCommands.add("");
            scrollOffset = 0;
            delayField.setText("20");
            savedDelay = "20";
            rebuild();
        }).dimensions(cx + 112, btnY, 40, 20).build());

        btnY += 26;

        // ── START ──
        this.addDrawableChild(ButtonWidget.builder(Text.literal(">> START"), b -> {
            saveAllFields();

            CommandSender sender = CommandSender.getInstance();

            try {
                sender.setDelayTicks(Integer.parseInt(delayField.getText().trim()));
            } catch (NumberFormatException e) {
                sender.setDelayTicks(20);
            }

            // Собираем ВСЕ непустые команды из savedCommands
            List<String> cmds = new ArrayList<>();
            for (String s : savedCommands) {
                String t = s.trim();
                if (!t.isEmpty()) {
                    cmds.add(t);
                }
            }

            System.out.println("[CommandSpammer] Sending " + cmds.size() + " commands to sender");

            sender.setCommands(cmds);
            sender.start();
            this.close();
        }).dimensions(cx - 152, btnY, 100, 20).build());

        // ── PAUSE ──
        this.addDrawableChild(ButtonWidget.builder(Text.literal("|| PAUSE"), b -> {
            CommandSender.getInstance().togglePause();
        }).dimensions(cx - 48, btnY, 100, 20).build());

        // ── STOP ──
        this.addDrawableChild(ButtonWidget.builder(Text.literal("[] STOP"), b -> {
            CommandSender.getInstance().stop();
        }).dimensions(cx + 56, btnY, 96, 20).build());
    }

    /**
     * Сохраняет текст из ВСЕХ полей обратно в savedCommands
     * Это ключевой метод — раньше он терял данные при скроллинге
     */
    private void saveAllFields() {
        if (allFields == null) return;

        // Обновляем только те строки, для которых есть поля
        for (int i = 0; i < allFields.size() && i < savedCommands.size(); i++) {
            TextFieldWidget field = allFields.get(i);
            if (field != null) {
                savedCommands.set(i, field.getText());
            }
        }

        if (delayField != null) {
            savedDelay = delayField.getText();
        }
    }

    private void pasteClipboard() {
        try {
            String clip = MinecraftClient.getInstance().keyboard.getClipboard();
            if (clip == null || clip.isEmpty()) return;
            String[] lines = clip.split("\\r?\\n");

            // Полностью заменяем список команд
            savedCommands.clear();
            for (String line : lines) {
                String trimmed = line.trim();
                savedCommands.add(trimmed); // добавляем даже пустые чтобы сохранить структуру
            }

            // Убираем пустые в конце
            while (savedCommands.size() > 1 && savedCommands.get(savedCommands.size() - 1).isEmpty()) {
                savedCommands.remove(savedCommands.size() - 1);
            }

            if (savedCommands.isEmpty()) {
                savedCommands.add("");
            }

            scrollOffset = 0;
            System.out.println("[CommandSpammer] Pasted " + savedCommands.size() + " lines from clipboard");
            rebuild();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void rebuild() {
        this.clearChildren();
        this.init();
    }

    @Override
    public void close() {
        saveAllFields();
        super.close();
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double amt) {
        saveAllFields(); // сохраняем перед скроллом!

        int maxScroll = Math.max(0, savedCommands.size() - VISIBLE_LINES);
        scrollOffset -= (int) amt;
        if (scrollOffset < 0) scrollOffset = 0;
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;

        rebuild();
        return true;
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        this.renderBackground(ctx);
        super.render(ctx, mx, my, delta);

        int cx = this.width / 2;

        ctx.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("\u00a7b\u00a7l=== Command Spammer ==="), cx, 8, 0xFFFFFF);

        ctx.drawTextWithShadow(this.textRenderer,
                Text.literal("\u00a7eDelay (ticks):"), cx - 60, 27, 0xFFFFFF);

        int totalLines = savedCommands.size();
        long filledLines = savedCommands.stream().filter(s -> !s.trim().isEmpty()).count();
        ctx.drawTextWithShadow(this.textRenderer,
                Text.literal("\u00a7aCommands: \u00a7f" + filledLines + "\u00a77/\u00a7f" + totalLines + " lines"),
                cx - 152, 47, 0xFFFFFF);

        // Нумерация
        int fieldsY = 77;
        for (int i = scrollOffset; i < Math.min(savedCommands.size(), scrollOffset + VISIBLE_LINES); i++) {
            int vi = i - scrollOffset;
            ctx.drawTextWithShadow(this.textRenderer,
                    Text.literal("\u00a77" + (i + 1)),
                    cx - 175, fieldsY + vi * 20 + 4, 0x888888);
        }

        // Скроллбар
        if (savedCommands.size() > VISIBLE_LINES) {
            int barX = cx + 156;
            int barTopY = fieldsY;
            int barHeight = VISIBLE_LINES * 20;
            int maxScroll = savedCommands.size() - VISIBLE_LINES;

            ctx.fill(barX, barTopY, barX + 4, barTopY + barHeight, 0x44FFFFFF);

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
                status = "PAUSED (" + s.getCurrentIndex() + "/" + s.getTotalCommands() + ")";
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

        ctx.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("\u00a78P = menu | Scroll = navigate"),
                cx, this.height - 8, 0x666666);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
