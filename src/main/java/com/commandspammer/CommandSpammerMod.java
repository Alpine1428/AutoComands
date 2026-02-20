package com.commandspammer;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import com.commandspammer.gui.CommandSpammerScreen;

public class CommandSpammerMod implements ClientModInitializer {

    private static KeyBinding openMenuKey;

    @Override
    public void onInitializeClient() {
        System.out.println("[CommandSpammer] Мод загружен!");

        openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "Open Command Spammer",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_P,
                "Command Spammer"
        ));

        // Инициализируем синглтон CommandSender (регистрирует tick listener)
        CommandSender.getInstance();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenuKey.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new CommandSpammerScreen());
                }
            }
        });
    }
}
