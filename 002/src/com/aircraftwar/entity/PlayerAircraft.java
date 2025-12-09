package com.aircraftwar.entity;

import com.aircraftwar.util.DrawUtil;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 玩家飞机类（修复：实现抽象方法move()，解决编译错误）
 */
public class PlayerAircraft extends Aircraft {
    private List<Bullet> bullets; // 玩家子弹列表
    private long lastShootTime;   // 上次发射时间
    private long shootInterval = 200; // 发射间隔（200ms）

    // 玩家飞机尺寸
    private static final int PLAYER_WIDTH = 40;
    private static final int PLAYER_HEIGHT = 50;

    public PlayerAircraft(int x, int y) {
        super(x, y, 5, 3, PLAYER_WIDTH, PLAYER_HEIGHT); // HP=5，速度=3
        this.bullets = new ArrayList<>();
        this.lastShootTime = System.currentTimeMillis();
    }

    // ========== 核心修复：实现Aircraft的抽象方法move() ==========
    @Override
    public void move() {
        // 玩家飞机的移动由moveUp/moveDown/moveLeft/moveRight控制，此处留空即可
        // 可选：添加边界检查兜底，防止玩家飞机越界
        x = Math.max(0, Math.min(800 - PLAYER_WIDTH, x));
        y = Math.max(0, Math.min(600 - PLAYER_HEIGHT, y));
    }

    // 发射子弹（控制发射间隔）
    public void shoot() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastShootTime >= shootInterval) {
            // 生成子弹（位置：玩家飞机顶部中间）
            bullets.add(new Bullet(x, y, PLAYER_WIDTH));
            lastShootTime = currentTime;
            // 播放射击音效（可选）
            com.aircraftwar.util.AudioUtil.playShootSound();
        }
    }

    // 更新子弹列表（移除死亡子弹，更新存活子弹）
    public void updateBullets() {
        Iterator<Bullet> iterator = bullets.iterator();
        while (iterator.hasNext()) {
            Bullet bullet = iterator.next();
            bullet.update();
            if (!bullet.isAlive()) {
                iterator.remove(); // 移除死亡子弹
            }
        }
    }

    // 绘制玩家飞机 + 子弹
    @Override
    public void draw(Graphics g) {
        if (isAlive()) {
            // 绘制玩家飞机
            DrawUtil.drawPlayerAircraft((Graphics2D) g, x, y, width, height);
            // 绘制所有存活子弹
            for (Bullet bullet : bullets) {
                bullet.draw(g);
            }
        }
    }

    // 玩家移动方法（边界限制）
    public void moveUp() {
        y -= speed;
        if (y < 0) y = 0;
    }

    public void moveDown(int panelHeight) {
        y += speed;
        if (y > panelHeight - height) y = panelHeight - height;
    }

    public void moveLeft() {
        x -= speed;
        if (x < 0) x = 0;
    }

    public void moveRight(int panelWidth) {
        x += speed;
        if (x > panelWidth - width) x = panelWidth - width;
    }

    @Override
    public void die() {
        // 玩家死亡音效
        com.aircraftwar.util.AudioUtil.playPlayerDeadSound();
    }

    // Getter
    public List<Bullet> getBullets() {
        return bullets;
    }
}