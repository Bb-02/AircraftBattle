package com.aircraftwar.entity;

import com.aircraftwar.util.AudioUtil;
import com.aircraftwar.util.DrawUtil;

import java.awt.*;
import java.util.Random;

/**
 * 敌机类（纯代码绘图）
 */
public class EnemyAircraft extends Aircraft {
    private static final Random random = new Random();
    // 敌机尺寸
    private static final int ENEMY_WIDTH = 30;
    private static final int ENEMY_HEIGHT = 40;

    public EnemyAircraft(int panelWidth, int panelHeight) {
        // 随机生成敌机位置（顶部外）
        super(random.nextInt(panelWidth - ENEMY_WIDTH), -ENEMY_HEIGHT,
                random.nextInt(3) + 2, 1, ENEMY_WIDTH, ENEMY_HEIGHT);
    }

    @Override
    public void move() {
        // 敌机向下移动
        y += speed;
    }

    @Override
    public void draw(Graphics g) {
        if (isAlive()) {
            DrawUtil.drawEnemyAircraft((Graphics2D) g, x, y, width, height);
        }
    }

    @Override
    public void die() {
        // 播放爆炸音效
        AudioUtil.playExplodeSound();
    }

    // 判断是否飞出屏幕
    public boolean isOutOfScreen(int panelHeight) {
        return y > panelHeight;
    }
}