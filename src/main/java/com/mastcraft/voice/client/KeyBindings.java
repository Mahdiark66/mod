package com.mastcraft.voice.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

/**
 * Standard Forge KeyMapping registration (1.21.1).
 */
public final class KeyBindings {

    public static KeyMapping PUSH_TO_TALK;
    public static KeyMapping WHISPER;
    public static KeyMapping GLOBAL;
    public static KeyMapping MUTE;

    private KeyBindings() {}

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        PUSH_TO_TALK = new KeyMapping(
                "key.mastcraftvoice.ptt",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                "key.categories.mastcraftvoice"
        );
        WHISPER = new KeyMapping(
                "key.mastcraftvoice.whisper",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                "key.categories.mastcraftvoice"
        );
        GLOBAL = new KeyMapping(
                "key.mastcraftvoice.global",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_N,
                "key.categories.mastcraftvoice"
        );
        MUTE = new KeyMapping(
                "key.mastcraftvoice.mute",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_M,
                "key.categories.mastcraftvoice"
        );

        event.register(PUSH_TO_TALK);
        event.register(WHISPER);
        event.register(GLOBAL);
        event.register(MUTE);
    }
}
