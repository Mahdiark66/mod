package com.mastcraft.voice.client.hud;

import com.mastcraft.voice.client.ClientVoiceManager;
import com.mastcraft.voice.network.VoicePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class VoiceHud {

    private final ClientVoiceManager manager;

    public VoiceHud(ClientVoiceManager manager) {
        this.manager = manager;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!manager.isHudEnabled()) return;
    }

    public void render(GuiGraphics g) {
        if (!manager.isHudEnabled()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        int x = 6;
        int y = 6;

        String modeName = switch (manager.getCurrentMode()) {
            case VoicePacket.MODE_WHISPER -> "Whisper";
            case VoicePacket.MODE_GLOBAL -> "Global";
            default -> "Proximity";
        };

        String mic = manager.isMicActive() ? "Mic: ON" : "Mic: OFF";
        int micColor = manager.isMicActive() ? 0x55FF55 : 0xFF5555;
        if (manager.isMuted()) {
            mic = "Mic: MUTED";
            micColor = 0xFF5555;
        }

        g.drawString(mc.font, "MastCraft Voice", x, y, 0xFFFFFF, true);
        g.drawString(mc.font, "Mode: " + modeName, x, y + 12, 0xCCCCCC, true);
        g.drawString(mc.font, mic, x, y + 24, micColor, true);
    }
}