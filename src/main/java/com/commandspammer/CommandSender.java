package com.commandspammer;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class CommandSender {

    private static CommandSender instance;

    private final List<String> commands = new CopyOnWriteArrayList<>();
    private int delayTicks = 20;
    private int currentIndex = 0;
    private int tickCounter = 0;
    private boolean running = false;
    private boolean paused = false;

    public static CommandSender getInstance() {
        if (instance == null) {
            instance = new CommandSender();
        }
        return instance;
    }

    private CommandSender() {
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    public void setCommands(List<String> cmds) {
        commands.clear();
        for (String cmd : cmds) {
            String trimmed = cmd.trim();
            if (!trimmed.isEmpty()) {
                commands.add(trimmed);
            }
        }
    }

    public void setDelayTicks(int ticks) {
        this.delayTicks = Math.max(1, ticks);
    }

    public int getDelayTicks() {
        return delayTicks;
    }

    public void start() {
        if (commands.isEmpty()) return;
        currentIndex = 0;
        tickCounter = 0;
        running = true;
        paused = false;
    }

    public void stop() {
        running = false;
        paused = false;
        currentIndex = 0;
        tickCounter = 0;
    }

    public void togglePause() {
        paused = !paused;
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isPaused() {
        return paused;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public int getTotalCommands() {
        return commands.size();
    }

    public List<String> getCommands() {
        return new ArrayList<>(commands);
    }

    private void tick(MinecraftClient client) {
        if (!running || paused) return;
        if (client.player == null || client.getNetworkHandler() == null) return;

        tickCounter++;

        if (tickCounter >= delayTicks) {
            tickCounter = 0;

            if (currentIndex < commands.size()) {
                String command = commands.get(currentIndex);
                sendCommand(client, command);
                currentIndex++;
            } else {
                running = false;
                currentIndex = 0;
                System.out.println("[CommandSpammer] Все команды отправлены!");
            }
        }
    }

    private void sendCommand(MinecraftClient client, String command) {
        if (client.player == null || client.getNetworkHandler() == null) return;

        try {
            if (command.startsWith("/")) {
                String cmd = command.substring(1);
                client.player.networkHandler.sendChatCommand(cmd);
            } else {
                client.player.networkHandler.sendChatMessage(command);
            }
            System.out.println("[CommandSpammer] Sent: " + command);
        } catch (Exception e) {
            System.err.println("[CommandSpammer] Error sending: " + command);
            e.printStackTrace();
        }
    }
}
