package com.aircraftwar.entity;

import com.aircraftwar.util.DrawUtil;

import java.awt.*;
import java.awt.geom.Ellipse2D;

/**
 * 子弹类（纯代码绘图）
 */
public class Bullet {
    private int x;
    private int y;
    private int size = 6; // 子弹尺寸
    private int speed;
    private int damage; // 伤害值
    private boolean alive;

    public Bullet(int x, int y, int speed, int damage) {
        this.x = x;
        this.y = y;
        this.speed = speed;
        this.damage = damage;
        this.alive = true;
    }

    // 移动
    public void move() {
        y -= speed;
        // 飞出屏幕则失效
        if (y < -size) {
            alive = false;
        }
    }

    // 绘制
    public void draw(Graphics g) {
        if (alive) {
            DrawUtil.drawBullet((Graphics2D) g, x, y, size);
        }
    }

    // 获取碰撞矩形
    public Rectangle getCollisionRect() {
        return new Rectangle(x, y, size, size);
    }

    // 击中目标
    public void hitTarget(Aircraft aircraft) {
        aircraft.hit(damage);
        alive = false;
    }

    // Getter
    public boolean isAlive() { return alive; }
}