package Plane_Game_2025;

import java.awt.*;

/**
 * 子弹类，负责子弹的移动和绘制
 */
public class Bullet {
    private int x;
    private int y;
    private int width;
    private int height;
    private int vx; // X方向速度
    private int vy; // Y方向速度
    private boolean isEnemyBullet; // 是否为敌人子弹（区分绘制颜色）
    private boolean alive;

    /**
     * 构造方法
     * @param x 初始X坐标
     * @param y 初始Y坐标
     * @param width 宽度
     * @param height 高度
     * @param vx X方向速度
     * @param vy Y方向速度
     * @param isEnemyBullet 是否为敌人子弹
     */
    public Bullet(int x, int y, int width, int height, int vx, int vy, boolean isEnemyBullet) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.vx = vx;
        this.vy = vy;
        this.isEnemyBullet = isEnemyBullet;
        this.alive = true;
    }

    /**
     * 子弹移动
     */
    public void move() {
        if (!alive) return;
        x += vx;
        y += vy;
    }

    /**
     * 绘制子弹（玩家子弹黄色，敌人子弹红色）
     */
    public void draw(Graphics2D g2d) {
        if (!alive) return;

        // 玩家子弹：亮黄色
        if (!isEnemyBullet) {
            g2d.setColor(new Color(255, 255, 0));
        } else {
            // 敌人子弹：粉红色
            g2d.setColor(new Color(255, 100, 150));
        }

        g2d.fillOval(x, y, width, height);
    }

    /**
     * 获取碰撞检测矩形
     */
    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }

    // Getter和Setter
    public int getX() { return x; }
    public int getY() { return y; }
    public boolean isAlive() { return alive; }
    public void setAlive(boolean alive) { this.alive = alive; }
    public boolean isEnemyBullet() { return isEnemyBullet; }
}