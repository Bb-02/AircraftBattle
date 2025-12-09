package com.aircraftwar.util;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

/**
 * 音频工具类（补全所有缺失的音频播放方法，处理加载异常）
 */
public class AudioUtil {
    // 音频文件路径（可根据实际路径调整，此处为相对路径示例）
    private static final String SHOOT_SOUND_PATH = "sounds/shoot.wav";
    private static final String EXPLODE_SOUND_PATH = "sounds/explode.wav";
    private static final String PLAYER_DEAD_SOUND_PATH = "sounds/player_dead.wav";
    private static final String GAME_OVER_SOUND_PATH = "sounds/game_over.wav";
    private static final String BG_MUSIC_PATH = "sounds/bg_music.wav";

    // 背景音乐剪辑（用于循环播放）
    private static Clip bgMusicClip;

    /**
     * 播放射击音效（玩家发射子弹）
     */
    public static void playShootSound() {
        playSound(SHOOT_SOUND_PATH);
    }

    /**
     * 播放爆炸音效（敌机被击中）
     */
    public static void playExplodeSound() {
        playSound(EXPLODE_SOUND_PATH);
    }

    /**
     * 播放玩家死亡音效（核心：补全缺失的方法）
     */
    public static void playPlayerDeadSound() {
        playSound(PLAYER_DEAD_SOUND_PATH);
    }

    /**
     * 播放游戏结束音效
     */
    public static void playGameOverSound() {
        playSound(GAME_OVER_SOUND_PATH);
    }

    /**
     * 播放背景音乐（循环）
     */
    public static void playBGM() {
        // 停止已有背景音乐
        if (bgMusicClip != null && bgMusicClip.isRunning()) {
            bgMusicClip.stop();
        }

        try {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new File(BG_MUSIC_PATH));
            bgMusicClip = AudioSystem.getClip();
            bgMusicClip.open(audioInputStream);
            // 设置循环播放（无限循环）
            bgMusicClip.loop(Clip.LOOP_CONTINUOUSLY);
            bgMusicClip.start();
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            // 音频文件不存在/加载失败时，打印提示但不影响游戏运行
            System.out.println("背景音乐加载失败：" + e.getMessage());
            // 降级处理：不播放背景音乐，避免游戏崩溃
        }
    }

    /**
     * 停止背景音乐
     */
    public static void stopBGM() {
        if (bgMusicClip != null && bgMusicClip.isRunning()) {
            bgMusicClip.stop();
            bgMusicClip.close();
        }
    }

    /**
     * 通用音频播放方法（处理异常，保证游戏不崩溃）
     */
    private static void playSound(String filePath) {
        new Thread(() -> { // 新开线程播放，避免阻塞游戏主线程
            try {
                File soundFile = new File(filePath);
                // 检查文件是否存在
                if (!soundFile.exists()) {
                    System.out.println("音频文件不存在：" + filePath);
                    return;
                }

                AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(soundFile);
                Clip clip = AudioSystem.getClip();
                clip.open(audioInputStream);
                clip.start();

                // 播放完毕后关闭资源
                clip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP) {
                        clip.close();
                        try {
                            audioInputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
            } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
                // 音频播放失败时，仅打印日志，不影响游戏核心逻辑
                System.out.println("音频播放失败：" + filePath + "，原因：" + e.getMessage());
            }
        }).start();
    }
}