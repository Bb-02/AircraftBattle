package com.aircraftwar.audio;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 简单的声音管理器：缓存/播放短音与 BGM（最小实现，线程安全的播放线程池）
 */
public class SoundManager {
    private static final SoundManager INSTANCE = new SoundManager();
    private ExecutorService soundPool = Executors.newCachedThreadPool();
    private Clip bgmClip;

    private SoundManager() {}

    public static SoundManager getInstance() { return INSTANCE; }

    public void playSound(final String filePath, final float volume) {
        soundPool.submit(() -> {
            try {
                File soundFile = new File(filePath);
                if (!soundFile.exists()) {
                    System.out.println("Sound file not found: " + filePath);
                    return;
                }
                AudioInputStream ais = AudioSystem.getAudioInputStream(soundFile);
                Clip clip = AudioSystem.getClip();
                clip.open(ais);
                setVolume(clip, volume);
                clip.start();
                clip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP) {
                        clip.close();
                        try { ais.close(); } catch (IOException ignored) {}
                    }
                });
            } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
                System.out.println("Failed to play sound: " + filePath + " -> " + e.getMessage());
            }
        });
    }

    public synchronized void playBgm(final String filePath, final boolean loop) {
        stopBgm();
        try {
            File f = new File(filePath);
            if (!f.exists()) {
                System.out.println("BGM file not found: " + filePath);
                return;
            }
            AudioInputStream ais = AudioSystem.getAudioInputStream(f);
            bgmClip = AudioSystem.getClip();
            bgmClip.open(ais);
            if (loop) bgmClip.loop(Clip.LOOP_CONTINUOUSLY);
            bgmClip.start();
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            System.out.println("Failed to play BGM: " + e.getMessage());
        }
    }

    public synchronized void stopBgm() {
        if (bgmClip != null && bgmClip.isRunning()) {
            bgmClip.stop();
            bgmClip.close();
            bgmClip = null;
        }
    }

    private void setVolume(Clip clip, float volume) {
        try {
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                float dB = (float) (20.0 * Math.log10(Math.max(0.0001, volume)));
                gain.setValue(dB);
            }
        } catch (Exception ignored) {}
    }
}

