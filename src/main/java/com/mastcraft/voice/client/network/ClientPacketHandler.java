package com.mastcraft.voice.client.network;

import com.mastcraft.voice.client.ClientVoiceManager;
import com.mastcraft.voice.network.VoicePacket;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.event.EventNetworkChannel;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

public class ClientPacketHandler {

    public static final ResourceLocation CHANNEL_ID =
            ResourceLocation.fromNamespaceAndPath("mastcraftvoice", "voice");

    private static final String PROTOCOL = "1";

    private static SimpleChannel CHANNEL;
    private static boolean READY = false;

    private final ClientVoiceManager manager;
    private int sequence = 0;

    public ClientPacketHandler(ClientVoiceManager manager) {
        this.manager = manager;
    }

    public static void registerChannel(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            try {
                CHANNEL = ChannelBuilder
                        .named(CHANNEL_ID)
                        .networkProtocolVersion(1)
                        .acceptedVersions((s, v) -> true)
                        .clientAcceptedVersions((s, v) -> true)
                        .serverAcceptedVersions((s, v) -> true)
                        .simpleChannel();

                CHANNEL.messageBuilder(VoicePayload.class, 0)
                        .decoder(VoicePayload::decode)
                        .encoder(VoicePayload::encode)
                        .consumerMainThread((msg, ctx) -> {
                            // inbound from server (if any)
                        })
                        .add();

                READY = true;
            } catch (Throwable t) {
                READY = false;
                System.err.println("[MastCraftVoice] Channel register failed: " + t.getMessage());
            }
        });
    }

    public void init() {
        // channel is registered in FMLCommonSetupEvent
    }

    public void sendVoiceData(byte mode, byte[] audio) {
        if (!READY || CHANNEL == null) return;
        if (audio == null || audio.length == 0 || audio.length > 28000) return;
        try {
            VoicePacket packet = VoicePacket.createClientData(
                    ++sequence,
                    System.currentTimeMillis(),
                    mode,
                    audio
            );
            CHANNEL.send(new VoicePayload(packet.encode()), PacketDistributor.SERVER.noArg());
        } catch (Throwable ignored) {
        }
    }

    public void sendControl(byte type, byte mode) {
        if (!READY || CHANNEL == null) return;
        try {
            VoicePacket packet = VoicePacket.createControl(
                    type,
                    ++sequence,
                    System.currentTimeMillis(),
                    mode
            );
            CHANNEL.send(new VoicePayload(packet.encode()), PacketDistributor.SERVER.noArg());
        } catch (Throwable ignored) {
        }
    }

    public void onServerPayload(byte[] raw) {
        try {
            manager.handleIncoming(raw);
        } catch (Exception ignored) {
        }
    }

    public static class VoicePayload implements CustomPacketPayload {
        public static final Type<VoicePayload> TYPE = new Type<>(CHANNEL_ID);
        private final byte[] data;

        public VoicePayload(byte[] data) {
            this.data = data != null ? data : new byte[0];
        }

        public static void encode(VoicePayload msg, RegistryFriendlyByteBuf buf) {
            buf.writeVarInt(msg.data.length);
            buf.writeBytes(msg.data);
        }

        public static VoicePayload decode(RegistryFriendlyByteBuf buf) {
            int len = buf.readVarInt();
            if (len < 0 || len > 65536) return new VoicePayload(new byte[0]);
            byte[] d = new byte[len];
            buf.readBytes(d);
            return new VoicePayload(d);
        }

        public void encode(RegistryFriendlyByteBuf buf) {
            encode(this, buf);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}