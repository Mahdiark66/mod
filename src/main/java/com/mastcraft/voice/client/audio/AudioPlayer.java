package com.mastcraft.voice.client.audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * One AudioPlayer per remote speaker.
 * Respects sequence numbers and discards old packets.
 */
public class AudioPlayer implements AutoCloseable {

    private final AudioCodec codec;
    private final ConcurrentLinkedQueue<QueuedFrame> queue = new ConcurrentLinkedQueue<>();
    private final AtomicInteger lastSequence = new AtomicInteger(-1);
    private SourceDataLine line;
    private Thread playThread;
    private volatile boolean running = true;
    private volatile float volume = 1.0f;

    public AudioPlayer(AudioCodec codec) {
        this.codec = codec;
        try {
            AudioFormat format = new AudioFormat(codec.getSampleRate(), 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();
            playThread = new Thread(this::playLoop, "MastCraft-Play");
            playThread.setDaemon(true);
            playThread.start();
        } catch (Exception e) {
            running = false;
        }
    }

    public void enqueue(int sequence, byte[] encoded, float volumeHint) {
        if (sequence <= lastSequence.get()) {
            return; // discard old
        }
        lastSequence.set(sequence);
        this.volume = Math.max(0f, Math.min(1f, volumeHint));
        queue.offer(new QueuedFrame(sequence, encoded));
    }

    private void playLoop() {
        while (running) {
            QueuedFrame frame = queue.poll();
            if (frame == null) {
                try {
                    Thread.sleep(2);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                continue;
            }
            byte[] pcm = codec.decode(frame.data);
            if (pcm.length == 0 || line == null) continue;

            // Simple volume scaling
            if (volume < 0.99f) {
                for (int i = 0; i < pcm.length - 1; i += 2) {
                    short sample = (short) ((pcm[i] & 0xFF) | (pcm[i + 1] << 8));
                    sample = (short) (sample * volume);
                    pcm[i] = (byte) (sample & 0xFF);
                    pcm[i + 1] = (byte) ((sample >> 8) & 0xFF);
                }
            }
            line.write(pcm, 0, pcm.length);
        }
    }

    @Override
    public void close() {
        running = false;
        if (playThread != null) {
            try {
                playThread.join(300);
            } catch (InterruptedException ignored) {
            }
        }
        if (line != null) {
            line.drain();
            line.stop();
            line.close();
        }
    }

    private static final class QueuedFrame {
        final int sequence;
        final byte[] data;
        QueuedFrame(int sequence, byte[] data) {
            this.sequence = sequence;
            this.data = data;
        }
    }
}
