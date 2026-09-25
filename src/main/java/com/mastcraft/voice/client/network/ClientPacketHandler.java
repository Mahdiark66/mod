package com.mastcraft.voice.client.network;

import com.mastcraft.voice.client.ClientVoiceManager;
import com.mastcraft.voice.network.VoicePacket;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Sends and receives voice data exclusively over the existing Minecraft connection
 * using Custom Payload packets. Paper receives these as plugin messages on the
 * channel "mastcraftvoice:voice".
 *
 * No UDP sockets, no WebSockets, no extra ports.
 */
public class ClientPacketHandler {

    public static final ResourceLocation CHANNEL_ID =
            ResourceLocation.fromNamespaceAndPath("mastcraftvoice", "voice");
    public static final String CHANNEL_STRING = "mastcraftvoice:voice";

    private final ClientVoiceManager manager;
    private int sequence;

    public ClientPacketHandler(ClientVoiceManager manager) {
        this.manager = manager;
    }

    public void init() {
        // Channel is registered implicitly when we send CustomPacketPayload
        // with the ResourceLocation that Paper has registered as a plugin channel.
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
        } catch (Exception e) {
            // swallow
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
        } catch (Exception e) {
            // ignore
        }
    }

    private void sendRaw(byte[] data) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null) return;
        if (data == null || data.length == 0) return;
        if (data.length > 30000) return;

        try {
            DiscardedPayload payload = new DiscardedPayload(CHANNEL_ID, Unpooled.wrappedBuffer(data));
            ServerboundCustomPayloadPacket packet = new ServerboundCustomPayloadPacket(payload);
            mc.getConnection().send(packet);
        } catch (Exception e) {
            try {
                FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
                DiscardedPayload payload = new DiscardedPayload(CHANNEL_ID, buf);
                mc.getConnection().send(new ServerboundCustomPayloadPacket(payload));
            } catch (Exception ignored) {
            }
        }
    }

    public void onServerPayload(byte[] raw) {
        manager.handleIncoming(raw);
    }
}
