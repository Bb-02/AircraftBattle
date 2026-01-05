package com.aircraftwar.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * 游戏主窗口
 */
public class GameFrame extends JFrame {
    private GamePanel gamePanel;

    public GameFrame() {
        // 窗口设置
        setTitle("飞机大战");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);

        // 创建游戏面板
        gamePanel = new GamePanel();
        add(gamePanel);

        // 自适应大小
        pack();

        // 居中显示
        setLocationRelativeTo(null);

        // 窗口关闭时停止游戏
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                gamePanel.stopGame();
            }
        });
    }
}