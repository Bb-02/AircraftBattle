package Plane_Game_2025;

import java.awt.*;

/**
 * 玩家飞机类，负责玩家飞机的移动、射击和绘制
 */
public class PlayerPlane {
    // 玩家飞机常量
    public static final int WIDTH = 40;
    public static final int HEIGHT = 40;
    private static final int BASE_SPEED = 5; // 基础移动速度
    private static final double SMOOTH_FACTOR = 0.15; // 平滑移动系数（越小越顺滑）

    // 位置和状态
    private int x;
    private int y;
    private int targetX; // 目标X坐标（鼠标位置）
    private int targetY; // 目标Y坐标（鼠标位置）
    private boolean alive;
    private long lastShootTime; // 上次射击时间（控制射击频率）

    public PlayerPlane(int x, int y) {
        this.x = x;
        this.y = y;
        this.targetX = x;
        this.targetY = y;
        this.alive = true;
        this.lastShootTime = 0;
    }

    /**
     * 平滑移动（转向减速效果）
     * 基于向量插值实现：飞机不会瞬间转向，而是逐渐逼近目标位置，距离越近速度越慢
     */
    public void move() {
        // 计算当前位置到目标位置的向量
        int dx = targetX - x;
        int dy = targetY - y;

        // 计算距离
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance > 1) { // 距离大于1才移动，避免抖动
            // 归一化向量（获取移动方向）
            double dirX = dx / distance;
            double dirY = dy / distance;

            // 计算移动量（距离越近，移动量越小，实现减速）
            double moveX = dirX * BASE_SPEED * Math.min(1.0, distance / 40);
            double moveY = dirY * BASE_SPEED * Math.min(1.0, distance / 40);

            // 更新位置
            x += (int) Math.round(moveX);
            y += (int) Math.round(moveY);

            // 边界限制（确保飞机不会超出屏幕）
            x = Math.max(0, Math.min(x, GamePanel.SCREEN_WIDTH - WIDTH));
            y = Math.max(0, Math.min(y, GamePanel.SCREEN_HEIGHT - HEIGHT));
        }
    }

    /**
     * 自动射击（朝向鼠标方向）
     */
    public Bullet shoot() {
        // 子弹初始位置（飞机中心）
        int bulletX = x + WIDTH / 2 - 2;
        int bulletY = y + HEIGHT / 2 - 2;

        // 计算子弹飞行方向（朝向目标位置）
        int dx = targetX + WIDTH / 2 - (x + WIDTH / 2);
        int dy = targetY + HEIGHT / 2 - (y + HEIGHT / 2);
        double distance = Math.sqrt(dx * dx + dy * dy);

        // 无目标时（距离为0）向上发射
        if (distance < 1) {
            return new Bullet(bulletX, bulletY, 4, 4, 0, -8, false);
        }

        // 子弹速度（8像素/帧）
        double speed = 8;
        int vx = (int) Math.round((dx / distance) * speed);
        int vy = (int) Math.round((dy / distance) * speed);

        return new Bullet(bulletX, bulletY, 4, 4, vx, vy, false);
    }

    /**
     * 绘制玩家飞机（青色三角形，视觉效果好）
     */
    public void draw(Graphics2D g2d) {
        if (!alive) return;

        // 绘制飞机主体（青色填充）
        g2d.setColor(new Color(0, 255, 255));
        int[] xPoints = {x + WIDTH/2, x, x + WIDTH};
        int[] yPoints = {y, y + HEIGHT, y + HEIGHT};
        g2d.fillPolygon(xPoints, yPoints, 3);

        // 绘制边框（白色）
        g2d.setColor(Color.WHITE);
        g2d.drawPolygon(xPoints, yPoints, 3);

        // 绘制中心亮点（增强视觉效果）
        g2d.setColor(new Color(255, 255, 255, 180));
        g2d.fillOval(x + WIDTH/2 - 3, y + HEIGHT/2 - 3, 6, 6);
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
    public boolean isAlive() { return alive; }
    public void setAlive(boolean alive) { this.alive = alive; }
    public void setTargetPosition(int targetX, int targetY) {
        this.targetX = targetX;
        this.targetY = targetY;
    }
    public long getLastShootTime() { return lastShootTime; }
    public void setLastShootTime(long lastShootTime) { this.lastShootTime = lastShootTime; }
}