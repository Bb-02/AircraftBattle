package com.aircraftwar.entity;

import com.aircraftwar.util.ImageUtil;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PlayerAircraft extends Aircraft {
    private List<Bullet> bullets;
    private long lastShootTime;
    private long shootInterval = 200;

    // 玩家飞机尺寸（和图片适配）
    private static final int PLAYER_WIDTH = 40;
    private static final int PLAYER_HEIGHT = 50;
    // 玩家飞机图片
    private BufferedImage playerImage;

    public PlayerAircraft(int x, int y) {
        super(x, y, 5, 3, PLAYER_WIDTH, PLAYER_HEIGHT);
        this.bullets = new ArrayList<>();
        this.lastShootTime = System.currentTimeMillis();
        // 加载玩家飞机图片 + 日志
        System.out.println("[PlayerAircraft] 开始加载 PlayerPlane.png");
        this.playerImage = ImageUtil.loadImage("PlayerPlane.png");
        if (this.playerImage == null) {
            System.out.println("[PlayerAircraft] ❌ PlayerPlane.png 加载失败！");
        } else {
            System.out.println("[PlayerAircraft] ✅ PlayerPlane.png 加载成功！");
        }
    }

    @Override
    public void move() {
        // 边界检查兜底
        x = Math.max(0, Math.min(800 - PLAYER_WIDTH, x));
        y = Math.max(0, Math.min(600 - PLAYER_HEIGHT, y));
    }

    public void shoot() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastShootTime >= shootInterval) {
            bullets.add(new Bullet(x, y, PLAYER_WIDTH));
            lastShootTime = currentTime;
            // AudioUtil.playShootSound(); // 保留音效（如有）
        }
    }

    public void updateBullets() {
        Iterator<Bullet> iterator = bullets.iterator();
        while (iterator.hasNext()) {
            Bullet bullet = iterator.next();
            bullet.update();
            if (!bullet.isAlive()) {
                iterator.remove();
            }
        }
    }

    @Override
    public void draw(Graphics g) {
        if (isAlive()) {
            // 核心修改：绘制图片替代矩形
            Graphics2D g2d = (Graphics2D) g;
            ImageUtil.drawImage(g2d, playerImage, x, y, PLAYER_WIDTH, PLAYER_HEIGHT);

            // 绘制子弹（原有逻辑不变）
            for (Bullet bullet : bullets) {
                bullet.draw(g);
            }
        }
    }

    // 其他方法（moveUp/moveDown等）不变
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
        // AudioUtil.playPlayerDeadSound(); // 保留音效（如有）
    }



    public List<Bullet> getBullets() {
        return bullets;
    }
}