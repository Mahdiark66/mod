package com.mastcraft.voice.client.network;

import com.mastcraft.voice.client.ClientVoiceManager;
import com.mastcraft.voice.network.VoicePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

/**
 * ارسال از طریق کانال Forge ثبت‌شده.
 * Paper آن را روی کانال mastcraftvoice:voice به‌صورت plugin message می‌گیرد.
 */
public class ClientPacketHandler {

    public static final ResourceLocation CHANNEL_ID =
            ResourceLocation.fromNamespaceAndPath("mastcraftvoice", "voice");

    private static SimpleChannel CHANNEL;

    private final ClientVoiceManager manager;
    private int sequence;
    private boolean ready = false;

    public ClientPacketHandler(ClientVoiceManager manager) {
        this.manager = manager;
    }

    public void init() {
        try {
            CHANNEL = ChannelBuilder
                    .named(CHANNEL_ID)
                    .networkProtocolVersion(1)
                    .clientAcceptedVersions(s -> true)
                    .serverAcceptedVersions(s -> true)
                    .simpleChannel();

            CHANNEL.messageBuilder(VoicePayload.class, 0, NetworkDirection.PLAY_TO_SERVER)
                    .codec(VoicePayload.STREAM_CODEC)
                    .consumerMainThread((payload, ctx) -> {
                        // سمت سرور Forge؛ روی Paper استفاده نمی‌شود
                    })
                    .add();

            CHANNEL.messageBuilder(VoicePayload.class, 1, NetworkDirection.PLAY_TO_CLIENT)
                    .codec(VoicePayload.STREAM_CODEC)
                    .consumerMainThread((payload, ctx) -> {
                        if (payload != null && payload.data != null) {
                            manager.handleIncoming(payload.data);
                        }
                    })
                    .add();

            ready = true;
        } catch (Exception e) {
            ready = false;
            System.err.println("[MastCraftVoice] Network init failed: " + e.getMessage());
        }
    }

    public void sendVoiceData(byte mode, byte[] audio) {
        if (!ready || CHANNEL == null) return;
        if (audio == null || audio.length == 0 || audio.length > 30000) return;

        try {
            VoicePacket packet = VoicePacket.createClientData(
                    ++sequence,
                    System.currentTimeMillis(),
                    mode,
                    audio
            );
            byte[] encoded = packet.encode();
            CHANNEL.send(new VoicePayload(encoded), PacketDistributor.SERVER.noArg());
        } catch (Exception e) {
            // هرگز نباید کانکشن را قطع کند
        }
    }

    public void sendControl(byte type, byte mode) {
        if (!ready || CHANNEL == null) return;
        try {
            VoicePacket packet = VoicePacket.createControl(
                    type,
                    ++sequence,
                    System.currentTimeMillis(),
                    mode
            );
            CHANNEL.send(new VoicePayload(packet.encode()), PacketDistributor.SERVER.noArg());
        } catch (Exception ignored) {
        }
    }

    public void onServerPayload(byte[] raw) {
        try {
            manager.handleIncoming(raw);
        } catch (Exception ignored) {
        }
    }

    public static final class VoicePayload implements CustomPacketPayload {
        public static final Type<VoicePayload> TYPE = new Type<>(CHANNEL_ID);

        public static final StreamCodec<FriendlyByteBuf, VoicePayload> STREAM_CODEC =
                StreamCodec.of(
                        (buf, payload) -> {
                            buf.writeVarInt(payload.data.length);
                            buf.writeBytes(payload.data);
                        },
                        buf -> {
                            int len = buf.readVarInt();
                            if (len < 0 || len > 65536) {
                                return new VoicePayload(new byte[0]);
                            }
                            byte[] data = new byte[len];
                            buf.readBytes(data);
                            return new VoicePayload(data);
                        }
                );

        public final byte[] data;

        public VoicePayload(byte[] data) {
            this.data = data != null ? data : new byte[0];
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}