package com.mastcraft.voice.network;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;

/**
 * Identical protocol to the server side.
 * Speaker UUID is only present when the packet is forwarded by the server.
 */
public final class VoicePacket {

    public static final byte PROTOCOL_VERSION = 1;

    public static final byte TYPE_VOICE_DATA = 0;
    public static final byte TYPE_VOICE_START = 1;
    public static final byte TYPE_VOICE_STOP = 2;
    public static final byte TYPE_VOICE_SETTINGS = 3;
    public static final byte TYPE_VOICE_PING = 4;

    public static final byte MODE_PROXIMITY = 0;
    public static final byte MODE_WHISPER = 1;
    public static final byte MODE_GLOBAL = 2;

    private final byte protocolVersion;
    private final byte packetType;
    private final int sequence;
    private final long timestamp;
    private final byte mode;
    private final float volumeHint;
    private final UUID speakerId;
    private final byte[] audioData;

    private VoicePacket(byte protocolVersion, byte packetType, int sequence, long timestamp,
                        byte mode, float volumeHint, UUID speakerId, byte[] audioData) {
        this.protocolVersion = protocolVersion;
        this.packetType = packetType;
        this.sequence = sequence;
        this.timestamp = timestamp;
        this.mode = mode;
        this.volumeHint = volumeHint;
        this.speakerId = speakerId;
        this.audioData = audioData != null ? audioData : new byte[0];
    }

    public static VoicePacket createClientData(int sequence, long timestamp, byte mode, byte[] audio) {
        return new VoicePacket(PROTOCOL_VERSION, TYPE_VOICE_DATA, sequence, timestamp, mode, 1.0f, null, audio);
    }

    public static VoicePacket createControl(byte type, int sequence, long timestamp, byte mode) {
        return new VoicePacket(PROTOCOL_VERSION, type, sequence, timestamp, mode, 1.0f, null, new byte[0]);
    }

    public byte[] encode() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);
        out.writeByte(protocolVersion);
        out.writeByte(packetType);
        out.writeInt(sequence);
        out.writeLong(timestamp);
        out.writeByte(mode);
        out.writeFloat(volumeHint);
        if (speakerId != null) {
            out.writeBoolean(true);
            out.writeLong(speakerId.getMostSignificantBits());
            out.writeLong(speakerId.getLeastSignificantBits());
        } else {
            out.writeBoolean(false);
        }
        out.writeInt(audioData.length);
        out.write(audioData);
        out.flush();
        return baos.toByteArray();
    }

    public static VoicePacket decode(byte[] data) throws IOException {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
        byte ver = in.readByte();
        if (ver != PROTOCOL_VERSION) {
            throw new IOException("Unsupported protocol version: " + ver);
        }
        byte type = in.readByte();
        int seq = in.readInt();
        long ts = in.readLong();
        byte mode = in.readByte();
        float vol = in.readFloat();
        UUID speaker = null;
        if (in.readBoolean()) {
            long most = in.readLong();
            long least = in.readLong();
            speaker = new UUID(most, least);
        }
        int len = in.readInt();
        if (len < 0 || len > 65536) {
            throw new IOException("Invalid audio length: " + len);
        }
        byte[] audio = new byte[len];
        in.readFully(audio);
        return new VoicePacket(ver, type, seq, ts, mode, vol, speaker, audio);
    }

    public byte getProtocolVersion() { return protocolVersion; }
    public byte getPacketType() { return packetType; }
    public int getSequence() { return sequence; }
    public long getTimestamp() { return timestamp; }
    public byte getMode() { return mode; }
    public float getVolumeHint() { return volumeHint; }
    public UUID getSpeakerId() { return speakerId; }
    public byte[] getAudioData() { return audioData; }
}
