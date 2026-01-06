package com.aircraftwar.entity;

import com.aircraftwar.util.DrawUtil;

import java.awt.*;

/**
 * 敌机子弹类
 */
public class EnemyBullet implements IBullet {
    private int x;
    private int y;
    private int size = 6; // 子弹尺寸
    private int speed = 5; // 子弹速度（向下）
    private boolean alive; // 是否存活
    private int damage = 1;

    public EnemyBullet(int x, int y) {
        this.x = x;
        this.y = y;
        this.alive = true;
    }

    // 实现 IBullet.update
    @Override
    public void update() {
        move();
    }

    // 移动（向下）
    public void move() {
        y += speed;
        // 飞出屏幕则失效
        if (y > 600) {
            alive = false;
        }
    }

    // 实现 IBullet.render
    @Override
    public void render(Graphics g) {
        draw(g);
    }

    // 绘制（红色子弹，区分玩家子弹）
    public void draw(Graphics g) {
        if (alive) {
            DrawUtil.drawEnemyBullet((Graphics2D) g, x, y, size);
        }
    }

    // 获取碰撞矩形
    @Override
    public Rectangle getCollisionRect() {
        return new Rectangle(x, y, size, size);
    }

    // Getter & Setter
    @Override
    public boolean isAlive() {
        return alive;
    }

    @Override
    public void setAlive(boolean alive) {
        this.alive = alive;
    }

    @Override
    public int getDamage() {
        return damage;
    }
}