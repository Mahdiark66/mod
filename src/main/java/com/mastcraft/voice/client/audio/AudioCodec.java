package com.mastcraft.voice.client.audio;

/**
 * Abstraction for audio encoding/decoding.
 * First version uses a simple PCM pass-through with optional light compression.
 * Later you can implement an OpusCodec without changing the networking layer.
 */
public interface AudioCodec {

    /**
     * Encode raw PCM (16-bit signed little-endian, mono) into a compact payload.
     */
    byte[] encode(byte[] pcm);

    /**
     * Decode payload back to PCM.
     */
    byte[] decode(byte[] encoded);

    /**
     * Recommended sample rate this codec expects.
     */
    int getSampleRate();

    /**
     * Frame size in milliseconds.
     */
    int getFrameMs();
}
