package com.aircraftwar.entity;

import com.aircraftwar.util.DrawUtil;

import java.awt.*;

/**
 * 玩家子弹类（修复：补充完整绘制逻辑，确保子弹可见）
 */
public class Bullet implements IBullet {
    private int x;
    private int y;
    private int speed = 8; // 子弹速度（向上）
    private int size = 6;  // 子弹尺寸（确保可见）
    private boolean alive = true; // 存活状态
    private int damage = 1;
    private final String type = "player_basic";
    private final int ownerId = 0;

    // 构造方法：子弹生成在玩家飞机顶部中间
    public Bullet(int playerX, int playerY, int playerWidth) {
        // 修正：子弹X坐标居中，避免偏移出屏幕
        this.x = playerX + (playerWidth / 2) - (size / 2);
        this.y = playerY - size; // 子弹在玩家飞机顶部生成
    }

    // 子弹移动（向上）
    public void move() {
        y -= speed;
        // 飞出屏幕顶部则标记为死亡
        if (y < 0) {
            alive = false;
        }
    }

    // 实现 IBullet.update
    @Override
    public void update() {
        if (alive) move();
    }

    // 击中目标后标记为死亡
    public void hitTarget(Aircraft target) {
        target.hit(damage);
        alive = false;
    }

    // 实现 IBullet.render
    @Override
    public void render(Graphics g) {
        draw(g);
    }

    // 绘制子弹（核心：补充绘制逻辑，黄色圆形子弹）
    public void draw(Graphics g) {
        if (alive) {
            DrawUtil.drawBullet((Graphics2D) g, x, y, size);
        }
    }

    // 更新子弹（移动+状态检查）
    public void updateLegacy() {
        if (alive) {
            move();
        }
    }

    // Getter & Setter
    @Override
    public boolean isAlive() {
        return alive;
    }

    @Override
    public Rectangle getCollisionRect() {
        return new Rectangle(x, y, size, size);
    }

    @Override
    public void setAlive(boolean alive) {
        this.alive = alive;
    }

    @Override
    public int getDamage() {
        return damage;
    }

    @Override
    public String getType() { return type; }

    @Override
    public int getOwnerId() { return ownerId; }
}