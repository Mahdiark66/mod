package com.mastcraft.voice.client.network;

import com.mastcraft.voice.client.ClientVoiceManager;
import com.mastcraft.voice.network.VoicePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.RegisterPayloadHandlersEvent;
import net.minecraftforge.network.registration.PayloadRegistrar;

public class ClientPacketHandler {

    public static final ResourceLocation CHANNEL_ID =
            ResourceLocation.fromNamespaceAndPath("mastcraftvoice", "voice");

    private static boolean REGISTERED = false;

    private final ClientVoiceManager manager;
    private int sequence = 0;

    public ClientPacketHandler(ClientVoiceManager manager) {
        this.manager = manager;
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        try {
            PayloadRegistrar registrar = event.registrar("1");
            registrar.optional().playToServer(
                    VoicePayload.TYPE,
                    VoicePayload.STREAM_CODEC,
                    (payload, context) -> {
                        // سمت سرور Forge؛ روی Paper استفاده نمی‌شود
                    }
            );
            registrar.optional().playToClient(
                    VoicePayload.TYPE,
                    VoicePayload.STREAM_CODEC,
                    (payload, context) -> {
                        // دریافت از سرور
                    }
            );
            REGISTERED = true;
        } catch (Throwable t) {
            REGISTERED = false;
            System.err.println("[MastCraftVoice] payload register failed: " + t.getMessage());
        }
    }

    public void init() {
    }

    public void sendVoiceData(byte mode, byte[] audio) {
        if (!REGISTERED) return;
        if (audio == null || audio.length == 0 || audio.length > 28000) return;
        try {
            VoicePacket packet = VoicePacket.createClientData(
                    ++sequence,
                    System.currentTimeMillis(),
                    mode,
                    audio
            );
            send(packet.encode());
        } catch (Throwable ignored) {
        }
    }

    public void sendControl(byte type, byte mode) {
        if (!REGISTERED) return;
        try {
            VoicePacket packet = VoicePacket.createControl(
                    type,
                    ++sequence,
                    System.currentTimeMillis(),
                    mode
            );
            send(packet.encode());
        } catch (Throwable ignored) {
        }
    }

    private void send(byte[] data) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.getConnection() == null) return;
            mc.getConnection().send(new ServerboundCustomPayloadPacket(new VoicePayload(data)));
        } catch (Throwable ignored) {
        }
    }

    public void onServerPayload(byte[] raw) {
        try {
            if (raw != null) manager.handleIncoming(raw);
        } catch (Exception ignored) {
        }
    }

    public static final class VoicePayload implements CustomPacketPayload {
        public static final Type<VoicePayload> TYPE = new Type<>(CHANNEL_ID);

        public static final StreamCodec<RegistryFriendlyByteBuf, VoicePayload> STREAM_CODEC =
                CustomPacketPayload.codec(VoicePayload::write, VoicePayload::new);

        private final byte[] data;

        public VoicePayload(byte[] data) {
            this.data = data != null ? data : new byte[0];
        }

        public VoicePayload(RegistryFriendlyByteBuf buf) {
            int len = buf.readVarInt();
            if (len < 0 || len > 65536) {
                this.data = new byte[0];
                return;
            }
            this.data = new byte[len];
            buf.readBytes(this.data);
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeVarInt(data.length);
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