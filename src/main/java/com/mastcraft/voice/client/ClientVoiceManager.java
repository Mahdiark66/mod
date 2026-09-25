package com.mastcraft.voice.client;

import com.mastcraft.voice.client.audio.AudioCodec;
import com.mastcraft.voice.client.audio.AudioPlayer;
import com.mastcraft.voice.client.audio.MicrophoneCapture;
import com.mastcraft.voice.client.audio.SimplePcmCodec;
import com.mastcraft.voice.client.hud.VoiceHud;
import com.mastcraft.voice.client.network.ClientPacketHandler;
import com.mastcraft.voice.network.VoicePacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ClientVoiceManager {

    private final AudioCodec codec = new SimplePcmCodec();
    private final Map<UUID, AudioPlayer> players = new ConcurrentHashMap<>();
    private final Map<UUID, Long> speakingUntil = new ConcurrentHashMap<>();

    private MicrophoneCapture mic;
    private ClientPacketHandler network;
    private byte currentMode = VoicePacket.MODE_PROXIMITY;
    private boolean hudEnabled = true;
    private boolean muted = false;

    public void init() {
        network = new ClientPacketHandler(this);
        network.init();

        try {
            mic = new MicrophoneCapture(codec, this::onMicFrame);
            mic.start();
        } catch (Exception e) {
            System.err.println("[MastCraftVoice] Microphone unavailable: " + e.getMessage());
        }

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new VoiceHud(this));
    }

    private void onMicFrame(byte[] encoded) {
        if (muted || mic == null || !mic.isPushToTalk()) return;
        network.sendVoiceData(currentMode, encoded);
    }

    public void handleIncoming(byte[] raw) {
        try {
            VoicePacket packet = VoicePacket.decode(raw);
            UUID speaker = packet.getSpeakerId();
            if (speaker == null) return;

            if (packet.getPacketType() == VoicePacket.TYPE_VOICE_DATA) {
                AudioPlayer player = players.computeIfAbsent(speaker, id -> new AudioPlayer(codec));
                player.enqueue(packet.getSequence(), packet.getAudioData(), packet.getVolumeHint());
                speakingUntil.put(speaker, System.currentTimeMillis() + 3000);
            } else if (packet.getPacketType() == VoicePacket.TYPE_VOICE_STOP) {
                speakingUntil.remove(speaker);
            }
        } catch (Exception ignored) {
        }
    }

    public boolean isSpeaking(UUID playerId) {
        Long until = speakingUntil.get(playerId);
        return until != null && until > System.currentTimeMillis();
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        boolean ptt = KeyBindings.PUSH_TO_TALK != null && KeyBindings.PUSH_TO_TALK.isDown();
        if (mic != null) {
            mic.setPushToTalk(ptt && !muted);
        }

        if (KeyBindings.WHISPER != null && KeyBindings.WHISPER.consumeClick()) {
            currentMode = VoicePacket.MODE_WHISPER;
            network.sendControl(VoicePacket.TYPE_VOICE_SETTINGS, currentMode);
        }
        if (KeyBindings.GLOBAL != null && KeyBindings.GLOBAL.consumeClick()) {
            currentMode = (currentMode == VoicePacket.MODE_GLOBAL)
                    ? VoicePacket.MODE_PROXIMITY
                    : VoicePacket.MODE_GLOBAL;
            network.sendControl(VoicePacket.TYPE_VOICE_SETTINGS, currentMode);
        }
        if (KeyBindings.MUTE != null && KeyBindings.MUTE.consumeClick()) {
            muted = !muted;
        }

        long now = System.currentTimeMillis();
        speakingUntil.entrySet().removeIf(e -> e.getValue() < now);
    }

    public void shutdown() {
        if (mic != null) {
            mic.close();
            mic = null;
        }
        players.values().forEach(AudioPlayer::close);
        players.clear();
        speakingUntil.clear();
    }

    public byte getCurrentMode() { return currentMode; }
    public boolean isMicActive() { return mic != null && mic.isPushToTalk() && !muted; }
    public boolean isHudEnabled() { return hudEnabled; }
    public void setHudEnabled(boolean v) { this.hudEnabled = v; }
    public boolean isMuted() { return muted; }
}
