package com.aircraftwar.util;

import java.awt.*;
import java.awt.geom.Ellipse2D;

/**
 * 图形绘制工具类（纯代码绘制游戏图形，无外部图片依赖）
 * 修正：Polygon类导入路径错误问题
 */
public class DrawUtil {

    /**
     * 绘制玩家飞机（三角形+矩形）
     */
    public static void drawPlayerAircraft(Graphics2D g2d, int x, int y, int width, int height) {
        // 设置抗锯齿
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 飞机主体（三角形）
        Polygon body = new Polygon();
        body.addPoint(x + width/2, y); // 顶部
        body.addPoint(x, y + height); // 左下
        body.addPoint(x + width, y + height); // 右下

        // 绘制主体
        g2d.setColor(Color.CYAN);
        g2d.fill(body);
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(2));
        g2d.draw(body);

        // 驾驶舱
        Ellipse2D cockpit = new Ellipse2D.Double(x + width/4, y + height/4, width/2, height/3);
        g2d.setColor(Color.WHITE);
        g2d.fill(cockpit);
    }

    /**
     * 绘制敌机（矩形+三角形）
     */
    public static void drawEnemyAircraft(Graphics2D g2d, int x, int y, int width, int height) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 敌机主体（矩形）
        Rectangle body = new Rectangle(x, y + height/4, width, height/2);
        g2d.setColor(Color.RED);
        g2d.fill(body);
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(2));
        g2d.draw(body);

        // 敌机头部（三角形）
        Polygon head = new Polygon();
        head.addPoint(x + width/2, y); // 顶部
        head.addPoint(x, y + height/4); // 左下
        head.addPoint(x + width, y + height/4); // 右下
        g2d.setColor(Color.RED);
        g2d.fill(head);
        g2d.setColor(Color.BLACK);
        g2d.draw(head);
    }

    /**
     * 绘制子弹（圆形）
     */
    public static void drawBullet(Graphics2D g2d, int x, int y, int size) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Ellipse2D bullet = new Ellipse2D.Double(x, y, size, size);
        g2d.setColor(Color.YELLOW);
        g2d.fill(bullet);
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(1));
        g2d.draw(bullet);
    }

    /**
     * 绘制爆炸效果（多个同心圆）
     */
    public static void drawExplosion(Graphics2D g2d, int x, int y, int size) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 外层大圆（橙色）
        Ellipse2D outer = new Ellipse2D.Double(x - size/2, y - size/2, size, size);
        g2d.setColor(new Color(255, 165, 0, 180)); // 半透明橙色
        g2d.fill(outer);

        // 中层圆（黄色）
        Ellipse2D middle = new Ellipse2D.Double(x - size/4, y - size/4, size/2, size/2);
        g2d.setColor(new Color(255, 255, 0, 200)); // 半透明黄色
        g2d.fill(middle);

        // 内层小圆（红色）
        Ellipse2D inner = new Ellipse2D.Double(x - size/8, y - size/8, size/4, size/4);
        g2d.setColor(new Color(255, 0, 0, 220)); // 半透明红色
        g2d.fill(inner);
    }
}