package com.aircraftwar;

import com.aircraftwar.ui.GameFrame;

import javax.swing.*;

/**
 * 游戏入口类（无外部资源依赖，可直接运行）
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