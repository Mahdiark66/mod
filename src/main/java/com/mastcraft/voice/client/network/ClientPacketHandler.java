package com.mastcraft.voice.client.network;

import com.mastcraft.voice.client.ClientVoiceManager;
import com.mastcraft.voice.network.VoicePacket;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public class ClientPacketHandler {

    public static final ResourceLocation CHANNEL_ID =
            ResourceLocation.fromNamespaceAndPath("mastcraftvoice", "voice");

    private final ClientVoiceManager manager;
    private int sequence;

    public ClientPacketHandler(ClientVoiceManager manager) {
        this.manager = manager;
    }

    public void init() {
    }

    public void sendVoiceData(byte mode, byte[] audio) {
        try {
            VoicePacket packet = VoicePacket.createClientData(
                    ++sequence,
                    System.currentTimeMillis(),
                    mode,
                    audio
            );
            sendRaw(packet.encode());
        } catch (Exception ignored) {
        }
    }

    public void sendControl(byte type, byte mode) {
        try {
            VoicePacket packet = VoicePacket.createControl(
                    type,
                    ++sequence,
                    System.currentTimeMillis(),
                    mode
            );
            sendRaw(packet.encode());
        } catch (Exception ignored) {
        }
    }

    private void sendRaw(byte[] data) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null) return;
        if (data == null || data.length == 0 || data.length > 30000) return;

        try {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            buf.writeBytes(data);
            CustomPacketPayload payload = new VoicePayload(buf);
            mc.getConnection().send(new ServerboundCustomPayloadPacket(payload));
        } catch (Exception ignored) {
        }
    }

    public void onServerPayload(byte[] raw) {
        manager.handleIncoming(raw);
    }

    public static final class VoicePayload implements CustomPacketPayload {
        public static final Type<VoicePayload> TYPE = new Type<>(CHANNEL_ID);
        private final byte[] data;

        public VoicePayload(FriendlyByteBuf buf) {
            this.data = new byte[buf.readableBytes()];
            buf.readBytes(this.data);
        }

        public VoicePayload(byte[] data) {
            this.data = data;
        }

        public void write(FriendlyByteBuf buf) {
            buf.writeBytes(data);
        }

        public byte[] data() {
            return data;
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}