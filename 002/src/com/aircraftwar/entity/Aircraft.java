package com.aircraftwar.entity;

import java.awt.*;

/**
 * 飞机抽象父类（定义抽象方法move()，所有子类必须实现）
 */
public abstract class Aircraft {
    protected int x;          // X坐标
    protected int y;          // Y坐标
    protected int speed;      // 移动速度
    protected int hp;         // 生命值
    protected int width;      // 宽度
    protected int height;     // 高度
    protected boolean alive = true; // 是否存活

    // 构造方法
    public Aircraft(int x, int y, int speed, int hp, int width, int height) {
        this.x = x;
        this.y = y;
        this.speed = speed;
        this.hp = hp;
        this.width = width;
        this.height = height;
    }

    // 抽象方法：移动（所有子类必须实现）
    public abstract void move();

    // 被击中扣血
    public void hit(int damage) {
        hp -= damage;
        if (hp <= 0) {
            alive = false;
            die();
        }
    }

    // 死亡回调（子类可重写）
    public void die() {}

    // 绘制方法（子类实现）
    public abstract void draw(Graphics g);

    // Getter & Setter
    public boolean isAlive() { return alive; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getHp() { return hp; }
    public void setHp(int hp) { this.hp = hp; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public Rectangle getCollisionRect() {
        return new Rectangle(x, y, width, height);
    }
}