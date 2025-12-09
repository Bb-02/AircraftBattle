package com.aircraftwar.entity;

import com.aircraftwar.util.DrawUtil;

import java.awt.*;

/**
 * 飞机抽象类，定义飞机的通用属性和行为（纯代码绘图）
 */
public abstract class Aircraft {
    // 位置和尺寸
    protected int x;
    protected int y;
    protected int width;
    protected int height;

    // 速度
    protected int speed;

    // 生命值
    protected int hp;

    // 构造方法
    public Aircraft(int x, int y, int speed, int hp, int width, int height) {
        this.x = x;
        this.y = y;
        this.speed = speed;
        this.hp = hp;
        this.width = width;
        this.height = height;
    }

    // 绘制飞机（抽象方法，由子类实现）
    public abstract void draw(Graphics g);

    // 获取碰撞矩形
    public Rectangle getCollisionRect() {
        return new Rectangle(x, y, width, height);
    }

    // 抽象方法：移动
    public abstract void move();

    // 被子弹击中
    public void hit(int damage) {
        hp -= damage;
        if (hp <= 0) {
            die();
        }
    }

    // 死亡处理
    public abstract void die();

    // Getter和Setter
    public int getX() { return x; }
    public void setX(int x) { this.x = x; }
    public int getY() { return y; }
    public void setY(int y) { this.y = y; }
    public int getHp() { return hp; }
    public boolean isAlive() { return hp > 0; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
}