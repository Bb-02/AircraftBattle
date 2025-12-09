package com.aircraftwar.util;

import java.awt.*;
import java.awt.geom.Ellipse2D;

/**
 * 图形绘制工具类（修复：调整drawExplosion参数，匹配调用）
 */
public class DrawUtil {
    // 绘制玩家飞机（蓝色矩形+白色描边）
    public static void drawPlayerAircraft(Graphics2D g2d, int x, int y, int width, int height) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 飞机主体
        g2d.setColor(Color.BLUE);
        g2d.fillRect(x, y, width, height);
        // 描边
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRect(x, y, width, height);
    }

    // 绘制敌机（红色矩形+黑色描边）
    public static void drawEnemyAircraft(Graphics2D g2d, int x, int y, int width, int height) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 敌机主体
        g2d.setColor(Color.RED);
        g2d.fillRect(x, y, width, height);
        // 描边
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRect(x, y, width, height);
    }

    // 绘制玩家子弹（黄色圆形）
    public static void drawBullet(Graphics2D g2d, int x, int y, int size) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Ellipse2D bullet = new Ellipse2D.Double(x, y, size, size);
        g2d.setColor(Color.YELLOW);
        g2d.fill(bullet);
        g2d.setColor(Color.ORANGE);
        g2d.setStroke(new BasicStroke(1));
        g2d.draw(bullet);
    }

    // 绘制敌机子弹（红色圆形）
    public static void drawEnemyBullet(Graphics2D g2d, int x, int y, int size) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Ellipse2D bullet = new Ellipse2D.Double(x, y, size, size);
        g2d.setColor(Color.RED);
        g2d.fill(bullet);
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(1));
        g2d.draw(bullet);
    }

    // ========== 核心修复：新增size参数，匹配调用的4个参数 ==========
    /**
     * 绘制爆炸效果
     * @param g2d 绘图上下文
     * @param x 爆炸中心X坐标
     * @param y 爆炸中心Y坐标
     * @param size 爆炸尺寸（适配调用时传入的第四个参数）
     */
    public static void drawExplosion(Graphics2D g2d, int x, int y, int size) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 爆炸动画（渐变圆形，尺寸由参数控制）
        GradientPaint gp = new GradientPaint(
                x, y, Color.ORANGE,
                x + size, y + size, Color.RED,
                true
        );
        g2d.setPaint(gp);
        g2d.fillOval(x - size/2, y - size/2, size, size);
    }
}