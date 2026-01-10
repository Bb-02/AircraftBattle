package com.aircraftwar.util;

import com.aircraftwar.audio.SoundManager;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

/**
 * 音频工具类（桥接到 SoundManager，保持向后兼容）
 */
public class AudioUtil {
    // 音频文件名（由 ResourceUtil 统一解析到工程根 sounds 目录）
    private static final String SHOOT_SOUND = "shoot.wav";
    private static final String EXPLODE_SOUND = "explode.wav";
    private static final String PLAYER_DEAD_SOUND = "player_dead.wav";
    private static final String GAME_OVER_SOUND = "game_over.wav";
    // 背景音乐：局内使用 Background.wav（位于工程根 sounds/Background.wav）
    private static final String BG_MUSIC = "Background.wav";

    // 菜单/非局内界面音乐（开始界面/排行榜/介绍界面等）
    private static final String MENU_MUSIC = "Menu.wav";

    // 兼容旧 API：播放射击音效（玩家发射子弹）
    public static void playShootSound() {
        SoundManager.getInstance().playSound(ResourceUtil.soundFile(SHOOT_SOUND).getPath(), 1.0f);
    }

    // 播放爆炸音效（敌机被击中）
    public static void playExplodeSound() {
        SoundManager.getInstance().playSound(ResourceUtil.soundFile(EXPLODE_SOUND).getPath(), 1.0f);
    }

    // 播放玩家死亡音效
    public static void playPlayerDeadSound() {
        SoundManager.getInstance().playSound(ResourceUtil.soundFile(PLAYER_DEAD_SOUND).getPath(), 1.0f);
    }

    // 播放游戏结束音效
    public static void playGameOverSound() {
        SoundManager.getInstance().playSound(ResourceUtil.soundFile(GAME_OVER_SOUND).getPath(), 1.0f);
    }

    // 播放背景音乐（循环）
    public static void playBGM() {
        SoundManager.getInstance().playBgm(ResourceUtil.soundFile(BG_MUSIC).getPath(), true);
    }

    // 播放菜单音乐（循环）
    public static void playMenuBGM() {
        SoundManager.getInstance().playBgm(ResourceUtil.soundFile(MENU_MUSIC).getPath(), true);
    }

    // 停止背景音乐
    public static void stopBGM() {
        SoundManager.getInstance().stopBgm();
    }

    // 旧实现保留为私有方法（不再使用），以防回退
    @SuppressWarnings("unused")
    private static void playSoundLegacy(String filePath) {
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