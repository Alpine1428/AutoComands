package com.commandspammer;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.List;

public class CommandSender {

    private static CommandSender instance;

    private List<String> commands = new ArrayList<>();
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
        commands = new ArrayList<>();
        for (String cmd : cmds) {
            String trimmed = cmd.trim();
            if (!trimmed.isEmpty()) {
                commands.add(trimmed);
            }
        }
        System.out.println("[CommandSpammer] Loaded " + commands.size() + " commands");
    }

    public void setDelayTicks(int ticks) {
        this.delayTicks = Math.max(1, ticks);
    }

    public int getDelayTicks() {
        return delayTicks;
    }

    public void start() {
        if (commands.isEmpty()) {
            System.out.println("[CommandSpammer] No commands to send!");
            return;
        }
        currentIndex = 0;
        tickCounter = 0;
        running = true;
        paused = false;
        System.out.println("[CommandSpammer] Started! " + commands.size() + " commands, delay=" + delayTicks + " ticks");
    }

    public void stop() {
        running = false;
        paused = false;
        currentIndex = 0;
        tickCounter = 0;
        System.out.println("[CommandSpammer] Stopped");
    }

    public void togglePause() {
        paused = !paused;
        System.out.println("[CommandSpammer] " + (paused ? "Paused" : "Resumed"));
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

                // Логируем прогресс каждые 10 команд
                if (currentIndex % 10 == 0) {
                    System.out.println("[CommandSpammer] Progress: " + currentIndex + "/" + commands.size());
                }
            } else {
                running = false;
                System.out.println("[CommandSpammer] All " + commands.size() + " commands sent!");
                currentIndex = 0;
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
        } catch (Exception e) {
            System.err.println("[CommandSpammer] Error sending command #" + currentIndex + ": " + command);
            e.printStackTrace();
        }
    }
}
