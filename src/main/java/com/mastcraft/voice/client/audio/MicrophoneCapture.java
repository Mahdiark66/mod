package com.mastcraft.voice.client.audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Captures microphone audio at 48 kHz mono, 20 ms frames.
 */
public class MicrophoneCapture implements AutoCloseable {

    private final AudioCodec codec;
    private final Consumer<byte[]> onFrame;
    private TargetDataLine line;
    private Thread captureThread;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean pushToTalk = new AtomicBoolean(false);

    public MicrophoneCapture(AudioCodec codec, Consumer<byte[]> onFrame) {
        this.codec = codec;
        this.onFrame = onFrame;
    }

    public void start() throws LineUnavailableException {
        if (running.get()) return;

        AudioFormat format = new AudioFormat(
                codec.getSampleRate(),
                16,
                1,
                true,
                false
        );
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        if (!AudioSystem.isLineSupported(info)) {
            throw new LineUnavailableException("Microphone line not supported");
        }
        line = (TargetDataLine) AudioSystem.getLine(info);
        line.open(format);
        line.start();

        running.set(true);
        captureThread = new Thread(this::captureLoop, "MastCraft-Mic");
        captureThread.setDaemon(true);
        captureThread.start();
    }

    private void captureLoop() {
        int bytesPerFrame = (codec.getSampleRate() * 2 * codec.getFrameMs()) / 1000; // 16-bit mono
        byte[] buffer = new byte[bytesPerFrame];

        while (running.get()) {
            if (!pushToTalk.get()) {
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                continue;
            }

            int read = line.read(buffer, 0, buffer.length);
            if (read > 0) {
                byte[] frame = new byte[read];
                System.arraycopy(buffer, 0, frame, 0, read);
                byte[] encoded = codec.encode(frame);
                if (encoded.length > 0) {
                    onFrame.accept(encoded);
                }
            }
        }
    }

    public void setPushToTalk(boolean active) {
        pushToTalk.set(active);
    }

    public boolean isPushToTalk() {
        return pushToTalk.get();
    }

    @Override
    public void close() {
        running.set(false);
        if (captureThread != null) {
            try {
                captureThread.join(500);
            } catch (InterruptedException ignored) {
            }
        }
        if (line != null) {
            line.stop();
            line.close();
        }
    }
}
