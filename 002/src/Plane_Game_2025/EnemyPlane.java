package Plane_Game_2025;

import java.awt.*;

/**
 * 敌人飞机类，负责敌人的移动、射击和绘制
 */
public class EnemyPlane {
    // 敌人飞机常量
    public static final int WIDTH = 36;
    public static final int HEIGHT = 36;
    private static final int SPEED = 2; // 移动速度

    // 位置和状态
    private int x;
    private int y;
    private boolean alive;

    public EnemyPlane(int x, int y) {
        this.x = x;
        this.y = y;
        this.alive = true;
    }

    /**
     * 朝向玩家移动
     */
    public void move(int playerCenterX, int playerCenterY) {
        if (!alive) return;

        // 计算敌人中心到玩家中心的向量
        int dx = playerCenterX - (x + WIDTH / 2);
        int dy = playerCenterY - (y + HEIGHT / 2);
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance > 0) {
            // 归一化向量，获取移动方向
            double dirX = dx / distance;
            double dirY = dy / distance;

            // 更新位置
            x += (int) Math.round(dirX * SPEED);
            y += (int) Math.round(dirY * SPEED);
        } else {
            // 玩家在正中心时，向下移动
            y += SPEED;
        }
    }

    /**
     * 敌人射击（向下发射子弹）
     */
    public Bullet shoot() {
        if (!alive) return null;

        // 子弹初始位置（敌人底部中心）
        int bulletX = x + WIDTH / 2 - 2;
        int bulletY = y + HEIGHT;

        // 敌人子弹向下飞行（vy=6）
        return new Bullet(bulletX, bulletY, 4, 4, 0, 6, true);
    }

    /**
     * 绘制敌人飞机（红色矩形，带白色边框）
     */
    public void draw(Graphics2D g2d) {
        if (!alive) return;

        // 绘制飞机主体（红色填充）
        g2d.setColor(new Color(255, 50, 50));
        g2d.fillRect(x, y, WIDTH, HEIGHT);

        // 绘制边框（白色）
        g2d.setColor(Color.WHITE);
        g2d.drawRect(x, y, WIDTH, HEIGHT);

        // 绘制敌人标志（白色十字）
        g2d.drawLine(x + WIDTH/2, y + 8, x + WIDTH/2, y + HEIGHT - 8);
        g2d.drawLine(x + 8, y + HEIGHT/2, x + WIDTH - 8, y + HEIGHT/2);
    }

    /**
     * 获取碰撞检测矩形
     */
    public Rectangle getBounds() {
        return new Rectangle(x, y, WIDTH, HEIGHT);
    }

    // Getter和Setter
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return WIDTH; }
    public int getHeight() { return HEIGHT; }
    public boolean isAlive() { return alive; }
    public void setAlive(boolean alive) { this.alive = alive; }
}