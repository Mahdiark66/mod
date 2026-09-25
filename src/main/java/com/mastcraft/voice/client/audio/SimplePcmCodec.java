package com.mastcraft.voice.client.audio;

import java.io.ByteArrayOutputStream;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * Lightweight pure-Java codec.
 * Uses DEFLATE for modest compression. Replace with Opus later.
 */
public class SimplePcmCodec implements AudioCodec {

    private static final int SAMPLE_RATE = 48000;
    private static final int FRAME_MS = 20;

    @Override
    public byte[] encode(byte[] pcm) {
        if (pcm == null || pcm.length == 0) return new byte[0];
        Deflater deflater = new Deflater(Deflater.BEST_SPEED);
        deflater.setInput(pcm);
        deflater.finish();
        ByteArrayOutputStream baos = new ByteArrayOutputStream(pcm.length / 2);
        byte[] buf = new byte[1024];
        while (!deflater.finished()) {
            int count = deflater.deflate(buf);
            baos.write(buf, 0, count);
        }
        deflater.end();
        return baos.toByteArray();
    }

    @Override
    public byte[] decode(byte[] encoded) {
        if (encoded == null || encoded.length == 0) return new byte[0];
        try {
            Inflater inflater = new Inflater();
            inflater.setInput(encoded);
            ByteArrayOutputStream baos = new ByteArrayOutputStream(encoded.length * 2);
            byte[] buf = new byte[1024];
            while (!inflater.finished()) {
                int count = inflater.inflate(buf);
                if (count == 0 && inflater.needsInput()) break;
                baos.write(buf, 0, count);
            }
            inflater.end();
            return baos.toByteArray();
        } catch (Exception e) {
            return new byte[0];
        }
    }

    @Override
    public int getSampleRate() {
        return SAMPLE_RATE;
    }

    @Override
    public int getFrameMs() {
        return FRAME_MS;
    }
}
