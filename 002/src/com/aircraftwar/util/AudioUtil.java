package com.aircraftwar.util;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream; // 正确导入ByteArrayInputStream

/**
 * 音效工具类（纯代码生成音效，无外部文件依赖）
 * 修正：1. ByteArrayInputStream包路径 2. multi-catch异常继承问题
 */
public class AudioUtil {
    // 背景音乐播放器
    private static Clip bgmClip;
    // 音频格式
    private static final AudioFormat AUDIO_FORMAT = new AudioFormat(44100, 16, 1, true, false);

    /**
     * 生成指定频率和时长的正弦波音效
     * @param frequency 频率（Hz）
     * @param duration 时长（毫秒）
     * @param volume 音量（0-1）
     */
    private static byte[] generateSineWave(int frequency, int duration, double volume) {
        int sampleRate = 44100;
        int samples = sampleRate * duration / 1000;
        byte[] data = new byte[samples * 2];
        double period = sampleRate / (double) frequency;

        for (int i = 0; i < samples; i++) {
            double angle = 2.0 * Math.PI * i / period;
            short sample = (short) (Math.sin(angle) * volume * Short.MAX_VALUE);
            data[2 * i] = (byte) (sample & 0xFF);
            data[2 * i + 1] = (byte) ((sample >> 8) & 0xFF);
        }
        return data;
    }

    /**
     * 播放射击音效（短音效）
     */
    public static void playShootSound() {
        playGeneratedSound(880, 100, 0.3); // 高音调，短时长
    }

    /**
     * 播放爆炸音效
     */
    public static void playExplodeSound() {
        new Thread(() -> {
            // 混合多个频率模拟爆炸
            playGeneratedSound(220, 200, 0.5);
            try { Thread.sleep(50); } catch (InterruptedException e) {}
            playGeneratedSound(110, 150, 0.4);
        }).start();
    }

    /**
     * 播放游戏结束音效
     */
    public static void playGameOverSound() {
        new Thread(() -> {
            playGeneratedSound(440, 300, 0.3);
            try { Thread.sleep(150); } catch (InterruptedException e) {}
            playGeneratedSound(220, 400, 0.3);
            try { Thread.sleep(200); } catch (InterruptedException e) {}
            playGeneratedSound(110, 500, 0.3);
        }).start();
    }

    /**
     * 播放背景音乐（循环的低频正弦波）
     */
    public static void playBGM() {
        stopBGM();
        try {
            // 生成循环的背景音乐数据（低频+渐变）
            byte[] bgmData = generateSineWave(110, 2000, 0.1); // 2秒的低频音效
            AudioInputStream audioStream = new AudioInputStream(
                    new ByteArrayInputStream(bgmData),
                    AUDIO_FORMAT,
                    bgmData.length / 2
            );

            DataLine.Info info = new DataLine.Info(Clip.class, AUDIO_FORMAT);
            bgmClip = (Clip) AudioSystem.getLine(info);
            bgmClip.open(audioStream);
            bgmClip.loop(Clip.LOOP_CONTINUOUSLY); // 循环播放
            bgmClip.start();
        } catch (LineUnavailableException e) {
            // 先捕获具体的音频异常
            System.err.println("音频线路不可用：" + e.getMessage());
        } catch (Exception e) {
            // 再捕获其他通用异常
            System.err.println("背景音乐播放失败：" + e.getMessage());
        }
    }

    /**
     * 停止背景音乐
     */
    public static void stopBGM() {
        if (bgmClip != null && bgmClip.isRunning()) {
            bgmClip.stop();
            bgmClip.close();
        }
    }

    /**
     * 播放生成的音效
     */
    private static void playGeneratedSound(int frequency, int duration, double volume) {
        try {
            byte[] soundData = generateSineWave(frequency, duration, volume);
            AudioInputStream audioStream = new AudioInputStream(
                    new ByteArrayInputStream(soundData),
                    AUDIO_FORMAT,
                    soundData.length / 2
            );

            DataLine.Info info = new DataLine.Info(Clip.class, AUDIO_FORMAT);
            Clip clip = (Clip) AudioSystem.getLine(info);
            clip.open(audioStream);
            clip.start();

            // 播放完毕后释放资源
            clip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) {
                    clip.close();
                    try { audioStream.close(); } catch (Exception e) {}
                }
            });
        } catch (LineUnavailableException e) {
            // 先捕获具体的音频异常
            System.err.println("音频线路不可用：" + e.getMessage());
        } catch (Exception e) {
            // 再捕获其他通用异常
            System.err.println("音效播放失败：" + e.getMessage());
        }
    }
}