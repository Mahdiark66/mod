package com.mastcraft.voice.client.network;

import com.mastcraft.voice.client.ClientVoiceManager;
import com.mastcraft.voice.network.VoicePacket;
import net.minecraft.resources.ResourceLocation;

/**
 * ارسال صدا فعلاً غیرفعال شده تا از کرش
 * EncoderException: custom_payload جلوگیری شود.
 * بعداً با ثبت صحیح Payload در Forge 1.21.1 فعال می‌شود.
 */
public class ClientPacketHandler {

    public static final ResourceLocation CHANNEL_ID =
            ResourceLocation.fromNamespaceAndPath("mastcraftvoice", "voice");

    private final ClientVoiceManager manager;
    private int sequence;
    private boolean sendEnabled = false; // فعلاً خاموش

    public ClientPacketHandler(ClientVoiceManager manager) {
        this.manager = manager;
    }

    public void init() {
        // ثبت شبکه بعداً اضافه می‌شود
    }

    public void sendVoiceData(byte mode, byte[] audio) {
        if (!sendEnabled) return;
        try {
            VoicePacket packet = VoicePacket.createClientData(
                    ++sequence,
                    System.currentTimeMillis(),
                    mode,
                    audio
            );
            // TODO: ارسال امن بعد از ثبت Payload
        } catch (Exception ignored) {
        }
    }

    public void sendControl(byte type, byte mode) {
        if (!sendEnabled) return;
        try {
            ++sequence;
        } catch (Exception ignored) {
        }
    }

    public void onServerPayload(byte[] raw) {
        try {
            manager.handleIncoming(raw);
        } catch (Exception ignored) {
        }
    }

    public void setSendEnabled(boolean enabled) {
        this.sendEnabled = enabled;
    }
}