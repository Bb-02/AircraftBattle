package com.aircraftwar;

import com.aircraftwar.ui.GameFrame;

import javax.swing.*;

/**
 * author: Bb-02
 * 游戏入口类
 * date: 2026/1/09
 */
public class GameMain {
    public static void main(String[] args) {
        // 在Swing事件调度线程中运行游戏
        SwingUtilities.invokeLater(() -> {
            GameFrame gameFrame = new GameFrame();
            gameFrame.setVisible(true);
        });
    }
}