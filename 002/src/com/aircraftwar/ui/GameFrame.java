package com.aircraftwar.ui;

import com.aircraftwar.factory.ProjectileFactory;

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

        // 注册一个示例弹种（laser）供后续使用/测试
        ProjectileFactory.register("laser", params -> {
            // 简单示例：复用 player_basic but could create LaserBullet in future
            return ProjectileFactory.create("player_basic", params);
        });

        // 初始化 UpgradeManager（确保其订阅分数变更事件）
        com.aircraftwar.upgrade.UpgradeManager.getInstance();

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