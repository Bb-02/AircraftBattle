package Plane_Game_2025;

import java.awt.*;

/**
 * 爆炸效果类，负责爆炸动画的绘制和生命周期管理
 */
public class Explosion {
    private int x; // 爆炸中心X坐标
    private int y; // 爆炸中心Y坐标
    private int radius; // 爆炸半径
    private int maxRadius; // 最大半径
    private int life; // 生命周期（帧数）
    private int maxLife; // 最大生命周期
    private boolean alive;

    public Explosion(int centerX, int centerY) {
        this.x = centerX;
        this.y = centerY;
        this.radius = 0;
        this.maxRadius = 35; // 最大爆炸半径
        this.life = 0;
        this.maxLife = 25; // 爆炸持续25帧（约0.4秒）
        this.alive = true;
    }

    /**
     * 更新爆炸状态（半径递增，生命周期递减）
     */
    public void update() {
        if (!alive) return;

        life++;
        // 半径随生命周期递增（先快后慢）
        radius = (int) (maxRadius * (1 - Math.exp(-life / 5.0)));

        // 生命周期结束，标记死亡
        if (life >= maxLife) {
            alive = false;
        }
    }

    /**
     * 绘制爆炸效果（渐变圆形，透明度递减）
     */
    public void draw(Graphics2D g2d) {
        if (!alive) return;

        // 计算透明度（随生命周期递减）
        int alpha = 255 - (255 * life / maxLife);
        if (alpha <= 0) return;

        // 外层爆炸圈（橙色，细边框）
        g2d.setColor(new Color(255, 150, 0, alpha));
        g2d.setStroke(new BasicStroke(3));
        g2d.drawOval(x - radius, y - radius, radius * 2, radius * 2);

        // 内层爆炸核心（红色，填充）
        g2d.setColor(new Color(255, 50, 0, alpha / 2));
        g2d.fillOval(x - radius / 2, y - radius / 2, radius, radius);
    }

    // Getter
    public boolean isAlive() { return alive; }
}