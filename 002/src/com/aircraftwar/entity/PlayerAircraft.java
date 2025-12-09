package com.aircraftwar.entity;

import com.aircraftwar.util.AudioUtil;
import com.aircraftwar.util.DrawUtil;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 玩家飞机类（纯代码绘图）
 */
public class PlayerAircraft extends Aircraft {
    // 子弹列表
    private List<Bullet> bullets;
    // 射击间隔
    private long lastShootTime;
    private static final long SHOOT_INTERVAL = 300; // 300毫秒

    // 飞机尺寸
    private static final int PLAYER_WIDTH = 40;
    private static final int PLAYER_HEIGHT = 50;

    public PlayerAircraft(int x, int y) {
        super(x, y, 8, 3, PLAYER_WIDTH, PLAYER_HEIGHT);
        bullets = new ArrayList<>();
        lastShootTime = System.currentTimeMillis();
    }

    @Override
    public void move() {
        // 玩家飞机由键盘控制，move方法空实现，移动逻辑在GamePanel中处理
    }

    // 向上移动
    public void moveUp() {
        y -= speed;
        if (y < 0) y = 0;
    }

    // 向下移动
    public void moveDown(int panelHeight) {
        y += speed;
        if (y > panelHeight - height) y = panelHeight - height;
    }

    // 向左移动
    public void moveLeft() {
        x -= speed;
        if (x < 0) x = 0;
    }

    // 向右移动
    public void moveRight(int panelWidth) {
        x += speed;
        if (x > panelWidth - width) x = panelWidth - width;
    }

    // 射击
    public void shoot() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastShootTime >= SHOOT_INTERVAL) {
            // 创建子弹（位置在飞机顶部中间）
            Bullet bullet = new Bullet(x + width/2 - 3, y - 10, 10, 1);
            bullets.add(bullet);
            // 播放射击音效
            AudioUtil.playShootSound();
            lastShootTime = currentTime;
        }
    }

    // 更新子弹
    public void updateBullets() {
        // 移除失效子弹
        bullets.removeIf(bullet -> !bullet.isAlive());
        // 更新子弹位置
        for (Bullet bullet : bullets) {
            bullet.move();
        }
    }

    // 绘制子弹
    public void drawBullets(Graphics g) {
        for (Bullet bullet : bullets) {
            bullet.draw(g);
        }
    }

    @Override
    public void draw(Graphics g) {
        if (isAlive()) {
            DrawUtil.drawPlayerAircraft((Graphics2D) g, x, y, width, height);
        }
    }

    @Override
    public void die() {
        // 播放爆炸音效
        AudioUtil.playExplodeSound();
        AudioUtil.playGameOverSound();
    }

    // 获取子弹列表
    public List<Bullet> getBullets() {
        return bullets;
    }
}